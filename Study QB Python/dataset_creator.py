from configs import *
from analysis import get_llm_response
from frequency import get_list_for_category
import json
import os
import time

def main():

    data_folder = Paths().data_folder

    consecutive_num_failed = 0
    max_num_failed = 3 # Exit after this many consecutive failed answer attempts (i.e. _get_prompt() returned none this many times), something's wrong

    for category in Category:
        
        category_string = category.value.lower().replace(" ", "_")
        category_folder = os.path.join(data_folder, category_string)

        if not os.path.exists(category_folder):
            os.mkdir(category_folder)

        answers = get_list_for_category(category)
        if answers is None:
            raise RuntimeError("Couldn't obtain frequency list for category. Maybe try again later.")

        for answer in answers:

            filename = answer.lower().replace(" ", "_")
            filepath = os.path.join(category_folder, f"{filename}.json")

            if os.path.exists(filepath):
                print(f"{filename} exists.")
                continue
            
            details, num_questions = _get_response(query=answer, category=category)

            if details is None:
                consecutive_num_failed += 1

                if consecutive_num_failed >= max_num_failed:
                    return
                
                continue

            data = {
                "answer": filename,
                "category": category_string,
                "details": details,
                "model": Configs().model,
                "num_questions_analyzed": num_questions
            }

            with open(filepath, "w", encoding="utf-8") as file:
                json.dump(data, file, indent=4)
            
            print(f"Finished {filename}.")
            consecutive_num_failed = 0
            


def _get_response(query: str, category: Category):

    wait_times = [61, 63, 65]
    wait_time_idx = 0
    response = "rate_limit_exceeded"

    while response == "rate_limit_exceeded":

        response, num_questions = get_llm_response(query, [category])

        if response == "rate_limit_exceeded":
            if wait_time_idx < len(wait_times):
                wait_time = wait_times[wait_time_idx]
                print(f"Rate limit reached; waiting {wait_time} seconds...")
                wait_time_idx += 1
                time.sleep(wait_time)
            else:
                print("Rate limit not reset after 1 min; the daily rate limit has likely been met; stopping...")
                response = None
        
    return response, num_questions
    

if __name__ == "__main__":
    main()