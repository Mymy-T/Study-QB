from api_key import API_KEY
from configs import Configs, Category
from groq import Groq, GroqError
import requests


def get_llm_response(query: str, categories=None):
    """
    Returns the response text from the LLM's analysis of clues, or None if there was an intermediate error.
    - TPM & TPD limits are much lower than RPM & RPD for ths use case, so rate-limit/retry on tokens
    """

    try:
        groq_client = Groq(api_key=API_KEY)

        prompt, num_questions = _get_prompt(query=query, categories=categories)
        
        if prompt is None:
            return (None, 0)

        response = groq_client.chat.completions.create(model=Configs().model, messages=[{"role": "user", "content": prompt}], stream=False)
        response_text = response.choices[0].message.content

        return (response_text, num_questions)

    except GroqError as error:

        raw_error_dict = getattr(error, "body", {})
        error_message = raw_error_dict.get("error", {}).get("message", "No error message.")
        error_type = raw_error_dict.get("error", {}).get("type", "No error type.")

        print(f"Groq Error!")
        print(f"MESSAGE: {error_message}")
        print(f"TYPE: {error_type}")
        print("\n")

        if error_type == "tokens":
            return ("rate_limit_exceeded", 0) # Don't skip over if it's just a rate limit, just reactivate timer

    except Exception as error:
        print(f"Unexpected error: {error}")
    
    return (None, 0)



def _get_prompt(query, categories):
    """
    Returns (prompt: str, num_questions_added: int) where num_questions_added is the num of questions to be analyzed by the model
    """

    if not categories:
        categories = [category.value for category in Category]
    
    difficulties_str = ",".join(map(str, Configs().difficulties))
    categories_str = ",".join(categories)
    
    url = "https://www.qbreader.org/api/query"
    params = {"queryString": query, "searchType": "answer", "exactPhrase": "true", "difficulties": difficulties_str, "categories": categories_str}
    
    try:
        # Get Questions (from QBReader)
        response = requests.get(url, params=params)
        response.raise_for_status()
        
        data = response.json().get("tossups", {}).get("questionArray", [])

        # Prompt Formation
        processedDataString = f"Given these quizbowl questions, explain the most important clues worth remembering. The clues should be specific. Clues that are frequent or involve significant names, objects, or processes should be prioritized. Try to list between 10 and 15 clues. The answer is {query}. Within the prompt, the questions will be delineated by the words END QUESTION. Order the clues so more common ones listed first. Use new lines (\n) to separate clues. Provide a description of each clue you found, in enough depth for someone new to the topic to understand. Do not explain why you chose the clues. Bold each clue. Do not bold the descriptions. Do not generate any extraneous information. The questions are as follows: "

        num_questions_added = 0
        for i in range(len(data)):
            answer = data[i].get("answer_sanitized", "not_found")
            question = data[i].get("question_sanitized", "not_found")

            if answer == "not_found" or question == "not_found":
                continue

            processedDataString += f"This is question number {i + 1}. The answer is {answer}. The question is as follows: {question} END QUESTION. "
            num_questions_added += 1

        if num_questions_added == 0:
            return (None, 0)

        return (processedDataString, num_questions_added)
        
    except requests.RequestException as e:
        print(f"An error occurred while fetching question data: {e}")
        return (None, 0)





if __name__ == "__main__":
    response, num_questions = get_llm_response("Pablo Neruda", categories=[Category.lit])
    print(f"RESPONSE: {response}")
    print(f"NUM QUESTIONS: {num_questions}")