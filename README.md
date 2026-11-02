# Study QB
Study QB is an app that allows for a streamlined studying process to improve at quizbowl. It can extract useful clues from questions, save them, and facilitate reviewing.

Features include...
 - Common clue analysis
 - Flashcards, with different clue review modes
 - Dynamic frequency lists
 - Spaced repetition
 - Review session tracking & stats
 - Customizable study reminders
 
## Installing
For iOS devices: https://apps.apple.com/us/app/study-qb/id6759358446
For Android devices: [https://play.google.com/apps/internaltest/4701523398543760954](https://play.google.com/apps/internaltest/4701523398543760954) (email must be on a list for this version: [https://forms.gle/o3AP3Sa7vPXmC33E6](https://forms.gle/o3AP3Sa7vPXmC33E6))

## Short Demos


| Clue Analysis | Spaced Repetition Review | Creating Notes |
| :---: | :---: | :---: |
| <img src="https://i.postimg.cc/d0tQ2Xry/Screen-Recording-11-01-2026-21-55-27-1-2.gif" width="160"/> | <img src="https://i.postimg.cc/2yYKvpLg/Screen-Recording-11-01-2026-22-20-18-1.gif" width="160" /> | <img src="https://i.postimg.cc/pLZCc0c1/Screen-Recording-11-01-2026-22-33-03-1.gif" width="160"/>


## Technologies
The iOS app was created using the Swift Programming Language. The SwiftUI and SwiftData libraries are utilized for the user interface and data flow respectively. The AI clue analysis models are accessed through the Groq API and analyzed questions are pulled using the QBReader API (https://www.qbreader.org).

The Android app was created using Kotlin.

A small set of Python scripts was utilized to generated a cached dataset for pre-analyzed questions. This would reduce the chance of hitting rate limits, leaving on-demand API calls for niche requests.
<!--stackedit_data:
eyJoaXN0b3J5IjpbLTE1OTA1MjQyNjNdfQ==
-->