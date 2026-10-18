from dataclasses import dataclass, field
from enum import StrEnum
import os

@dataclass
class Configs:
    model: str = "openai/gpt-oss-120b"
    num_per_category: int = 100 # May not be perfectly 25 if there are errors in getting freq list or tossup processing
    difficulties: list = field(default_factory=lambda: [2, 3, 4, 5]) # High school difficulty


class Paths:
    def __init__(self):
        
        configs = Configs()

        base_folder = os.path.dirname(os.path.abspath(__file__)) # paths.py should be on same level as other files
        base_data_folder = os.path.join(base_folder, "data")

        self.data_folder = os.path.join(base_data_folder, f"dataset_top_{configs.num_per_category}")

        if not os.path.exists(base_data_folder):
            os.mkdir(base_data_folder)
        
        if not os.path.exists(self.data_folder):
            os.mkdir(self.data_folder)

    
class Category(StrEnum):
    lit = "Literature"
    hist = "History"
    sci = "Science"
    art = "Fine Arts"
    rel = "Religion"
    myth = "Mythology"
    phil = "Philosophy"
    ss = "Social Science"
    ce = "Current Events"
    geo = "Geography"