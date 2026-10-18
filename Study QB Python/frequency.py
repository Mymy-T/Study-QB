from configs import Configs, Category
import requests

def get_list_for_category(category: Category):
    """
    Returns the list of answers based on the top x of the frequency list (determined by limit param in request), or None if there was a request error.
    """

    difficulties = ",".join(map(str, Configs().difficulties))
    category_str = category.value

    url = "https://www.qbreader.org/api/frequency-list"
    params = {"category": category_str, "difficulties": difficulties, "limit": Configs().num_per_category}

    try:
        response = requests.get(url, params=params)
        response.raise_for_status()

        freq_list = response.json().get("frequencyList", [])

        answers = []
        for item in freq_list:
            answer = item.get("answer", None)
            if answer is None:
                print(f"Error occured while processing this frequency list item: {item}")
                return None
           
            answers.append(answer)
        
        return answers

    except requests.RequestException as e:
        print(f"An error occured while fetching a frequency list: {e}")
        return None


if __name__ == "__main__":
    answers = get_list_for_category(Category.lit)
    print(answers)