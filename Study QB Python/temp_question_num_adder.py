from analysis import _get_prompt
from configs import Category
import json
import os

basepath = os.path.join(os.getcwd(), "data/dataset_top_100/literature") # Run from base folder path

paths = list(os.listdir(basepath))
paths = [os.path.join(basepath, x) for x in paths]

for path in paths:

    data = "placeholder"

    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
        prompt, num = _get_prompt(data["answer"].replace("_", " "), Category.lit)
        data["num_questions_analyzed"] = num
    

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=4) 

print("Question adding complete!")