from dataclasses import dataclass, field
from enum import StrEnum
import os

@dataclass
class Configs:
    # General
    num_per_category: int = 100 # May not be perfectly equal if there are errors in getting freq list or tossup processing
    difficulties: list = field(default_factory=lambda: [2, 3, 4, 5]) # High school difficulty

    # Groq
    model: str = "openai/gpt-oss-120b"

    # Claude
    claude_model: str = "opus" # Options: "opus", "sonnet", "haiku", "fable"
    claude_model_ids: dict = field(default_factory=lambda: {"opus": "claude-opus-5-5", "sonnet": "claude-sonnet-5-5", "haiku": "claude-haiku-5-5", "fable": "claude-fable-5-1"})
    num_agents: int = 5 # Max number of `claude -p` processes running at once; 1 = sequential like dataset_creator.py
    claude_timeout_seconds: int = 600
    min_clues: int = 10
    max_clues: int = 15
    claude_system_prompt: str = (
        "You analyze quizbowl questions and write study clues. Follow the user's instructions literally. "
        "List 10 to 15 clues, never more than 15 (count them before answering), the ones appearing most often across the provided questions first. "
        "Do not add a catch-all clue (e.g. \"Other works\"). Make clues specific: names, titles, quotes, objects, events, processes. "
        "Each clue goes on its own line in **bold**. The next line is a description that is not bolded and is detailed enough for a newcomer to understand (usually 1 to 3 sentences). "
        "Put one blank line between clues. No intro, no closing text, no headings, no numbering. Do not explain why you chose the clues. "
        "Base the clues on the provided questions. Descriptions may add accurate background knowledge, but never invent facts; if you are unsure of a detail, leave it out. "
        "Silently correct obvious typos and factual errors in the source questions, but only when you are confident. "
        "If some tossups are clearly about a different answer (e.g. an exact-phrase match on a different subject), base the clues on the questions that match the stated answer."
    )  # Extra answer rules used for the current dataset; set to None to send _get_prompt()'s text exactly as Groq received it


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