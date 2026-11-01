# Privacy Policy — Study QB

**Last updated:** 17 September 2026

Study QB ("the app") is a quizbowl study tool. This policy explains what the app does with your information. It is written to be accurate about the app's actual behavior rather than to cover hypothetical cases.

## Summary

- The app has **no accounts and no login**. It never asks for your name, email, or any identifier.
- Your study cards and statistics are stored **only on your device**.
- Two features send text to outside services in order to work: the **Analyze** feature and the **Buzz In** practice mode. What they send is described below.
- The app contains **no advertising, no analytics, and no crash-reporting or tracking SDKs**.
- Nothing is sold, rented, or shared for marketing purposes.

## What is stored on your device

The app stores the following locally, in a database on your device, and nowhere else:

- The study cards you create — answer lines, categories, personal notes, details text, and colors
- Your review history and spaced-repetition scheduling state
- Buzz In performance statistics — points, correct and incorrect counts, and average buzz position per card
- Your app settings, including study reminder preferences

This data is not transmitted to the developer. It is removed when you delete the app, and you can erase it at any time from **Settings → Delete all cards**.

## What leaves your device

The app communicates with exactly two external services, both over encrypted HTTPS connections.

### qbreader.org

When you use the **Analyze** feature or view a frequency list, the app requests quizbowl question data from the public qbreader.org API. The app sends the search term you type and your selected category and difficulty filters. No personal information is attached to these requests.

### Groq

Groq provides the language-model processing behind two features:

- **Analyze** — the answer line or keyword you type, together with the quizbowl question text retrieved from qbreader.org, is sent to Groq so it can summarize the clues that commonly appear for that answer.
- **Buzz In** — when generating a practice question, the card's answer line and the clue lines from that card's personal notes are sent to Groq so it can compose a practice tossup.

This means **text you have written in your cards can leave your device** when you use Buzz In. It is sent without any account identifier, device identifier, or other information that would link it to you. If you would prefer that your notes never leave the device, do not use the Buzz In practice mode; the Classic review mode operates entirely offline.

Groq's handling of the data it receives is governed by its own privacy policy, available at <https://groq.com/privacy-policy/>.

## Permissions the app requests

- **Internet** — required for the Analyze and Buzz In features described above
- **Notifications** — to show the study reminders you configure in Settings
- **Run at startup** — so that reminders you have already scheduled survive a device restart

The app does not request access to your location, contacts, camera, microphone, photos, or files.

## Children

Study QB is a general-purpose study tool and is not directed at children under 13. It does not knowingly collect information from children.

## Changes to this policy

If the app's data handling changes, this page will be updated and the date at the top revised.

## Contact

Questions about this policy can be sent to **feilin.physics@gmail.com**.
