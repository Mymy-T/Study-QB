from configs import Category, Paths
import json
import os

def main():
    
    base_data_folder = Paths().base_data_folder

    data_folders = [x for x in list(os.listdir(base_data_folder)) if not x.startswith(".")]

    for data_folder in data_folders:

        save_path = os.path.join(base_data_folder, data_folder, "dataset.json")
        data = {
            "items": []
        }

        for category in Category:

            category_folder = os.path.join(base_data_folder, data_folder, category.value.lower().replace(" ", "_"))
            files = [x for x in list(os.listdir(category_folder)) if x.endswith(".json")]

            for file in files:

                file_path = os.path.join(category_folder, file)

                with open(file_path, "r", encoding="utf-8") as f:
                    data["items"].append(json.load(f))
        
        with open(save_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4)
        
        print(f"Processing for {data_folder} is complete!")
    
    print("All processing completed!")


if __name__ == "__main__":
    main()