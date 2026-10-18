from analysis import _get_prompt
import asyncio
from configs import Configs, Paths, Category
from frequency import get_list_for_category
import json
import os


async def main():

    data_folder = Paths().data_folder
    num_agents = Configs().num_agents

    jobs = asyncio.Queue()
    queued_paths = set()

    for category in Category:

        category_string = category.value.lower().replace(" ", "_")
        category_folder = os.path.join(data_folder, category_string)

        if not os.path.exists(category_folder):
            os.mkdir(category_folder)

        answers = await asyncio.to_thread(get_list_for_category, category)

        if answers is None:
            raise RuntimeError("Couldn't obtain frequency list for category. Maybe try again later.")


        for answer in answers:

            filename = answer.lower().replace(" ", "_")
            filepath = os.path.join(category_folder, f"{filename}.json")

            if "/" in filename: # e.g. "either/or" would be an invalid path
                print(f"{filename} has an invalid filename; skipping.")
                continue

            if os.path.exists(filepath) or filepath in queued_paths:
                print(f"{filename} exists.")
                continue

            queued_paths.add(filepath)
            jobs.put_nowait((category, category_string, answer, filename, filepath))

    print(f"{jobs.qsize()} answers to generate with up to {num_agents} agents.")

    # Shared state (safe without locks: asyncio runs on one thread)
    state = {"consecutive_num_failed": 0, "started": 0, "written": 0, "busy": 0}
    max_num_failed = 3 # Stop starting new answers after this many consecutive failures, something's wrong
    stop = asyncio.Event()

    async def worker():

        while not stop.is_set() and not jobs.empty():

            category, category_string, answer, filename, filepath = jobs.get_nowait()
            state["started"] += 1
            state["busy"] += 1
            print(f"Started {filename}. ({state['busy']}/{num_agents} agents busy)")

            details, num_questions, model_id = await _get_response(query=answer, category=category, stop=stop)
            state["busy"] -= 1

            if details == "no_questions":
                print(f"{filename} has no tossups to analyze; skipping.")
                continue

            if details is None:
                state["consecutive_num_failed"] += 1

                if state["consecutive_num_failed"] >= max_num_failed and not stop.is_set():
                    print(f"{max_num_failed} consecutive failures; not starting any new answers.")
                    stop.set()

                continue

            data = {
                "answer": filename,
                "category": category_string,
                "details": details,
                "model": model_id,
                "num_questions_analyzed": num_questions
            }

            with open(filepath, "w", encoding="utf-8") as file:
                json.dump(data, file, indent=4)

            state["consecutive_num_failed"] = 0
            state["written"] += 1
            print(f"Finished {filename}. ({state['busy']}/{num_agents} agents busy)")

    await asyncio.gather(*(worker() for _ in range(num_agents)))

    print(f"Done: {state["written"]} written, {jobs.qsize()} not started.")


async def _get_response(query: str, category: Category, stop: asyncio.Event):

    wait_times = [61, 63, 65]
    wait_time_idx = 0
    response = "rate_limit_exceeded"

    while response == "rate_limit_exceeded":

        response, num_questions, model_id = await get_claude_response(query, [category])

        if response == "rate_limit_exceeded":
            if wait_time_idx < len(wait_times):
                wait_time = wait_times[wait_time_idx]
                print(f"Rate limit reached; waiting {wait_time} seconds...")
                wait_time_idx += 1
                await asyncio.sleep(wait_time)
            else:
                print("Rate limit not reset after 1 min; the usage limit has likely been met; stopping...")
                stop.set() # A usage limit affects every agent
                response = None

    return response, num_questions, model_id


async def get_claude_response(query: str, categories):
    """
    Returns (response_text, num_questions, model_id); response_text is None on error, "rate_limit_exceeded" if rate limited, or "no_questions" if QBReader has no tossups for the answer
    """

    configs = Configs()

    prompt, num_questions = await asyncio.to_thread(_get_prompt, query=query, categories=categories)

    if prompt is None:
        return ("no_questions", 0, None)

    for attempt in range(2): # Retry once if the output doesn't have num clues in range [min_clues, max_clues]
        text, model_id = await _call_claude(prompt)

        if text is None or text == "rate_limit_exceeded":
            return (text, 0, model_id)

        num_clues = sum(1 for line in text.splitlines() if line.startswith("**"))
        if text.startswith("**") and configs.min_clues <= num_clues <= configs.max_clues:
            return (text, num_questions, model_id)

        print(f"Output for {query} had {num_clues} clues (expected {configs.min_clues}-{configs.max_clues}); {'retrying' if attempt == 0 else 'giving up'}.")

    return (None, 0, None) # Only reached if the number of clues generated keeps failing, not if there's a generation error (that breaks out immediately)


async def _call_claude(prompt: str):
    """
    Sends the prompt to `claude -p` with all tools disabled and returns (text, model_id).
    """

    configs = Configs()

    command = ["claude", "-p", "--model", configs.claude_model, "--output-format", "json", "--tools", "", "--no-session-persistence"]

    if configs.claude_system_prompt:
        command += ["--system-prompt", configs.claude_system_prompt]

    env = {key: value for key, value in os.environ.items() if key != "CLAUDECODE"} # Allows running from inside a Claude Code session

    try:
        process = await asyncio.create_subprocess_exec(*command, stdin=asyncio.subprocess.PIPE, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE, env=env)
    except FileNotFoundError:
        print("The `claude` CLI was not found on PATH.")
        return (None, None)

    try:
        stdout, stderr = await asyncio.wait_for(process.communicate(prompt.encode("utf-8")), configs.claude_timeout_seconds)
    except asyncio.TimeoutError:
        process.kill()
        await process.wait()
        print(f"Claude did not respond within {configs.claude_timeout_seconds} seconds.")
        return (None, None)

    stdout, stderr = stdout.decode("utf-8", errors="replace"), stderr.decode("utf-8", errors="replace")

    try:
        output = json.loads(stdout)
    except json.JSONDecodeError:
        output = {"is_error": True, "result": (stdout + stderr).strip()}

    message = str(output.get("result", ""))

    if process.returncode != 0 or output.get("is_error"):
        print(f"Claude Error!")
        print(f"MESSAGE: {message or stderr.strip() or 'No error message.'}")
        print("\n")

        if "limit" in message.lower() or "overloaded" in message.lower():
            return ("rate_limit_exceeded", None) # Don't skip over if it's just a rate limit, just reactivate timer
        return (None, None)

    # Record the model that actually answered (the one with the most output tokens)
    model_usage = output.get("modelUsage") or {}
    model_id = max(model_usage, key=lambda name: model_usage[name].get("outputTokens", 0)) if model_usage else configs.claude_model_ids[configs.claude_model]

    return (message.strip(), model_id)


if __name__ == "__main__":
    asyncio.run(main())
