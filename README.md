# verbum

Android widget that shows fresh word cards every day. GitHub Actions renders the cards from your own word list and pushes them to a `daily` branch. The app syncs them into gallery albums so you can pin them as a widget.

Bring your own word list. Topic-agnostic.

## How it works

```
words.json -> GitHub Actions (daily) -> output/ -> daily branch
                                                        |
                                            Android app syncs via API
                                                        |
                                            Gallery albums -> widget
```

## Setup

**1. Fork this repo**

Actions run on your fork automatically.

**2. Edit `words.json`**

Each entry needs four fields:

```json
{
  "word": "ephemeral",
  "pos": "adjective",
  "ipa": "/ɪˈfɛm.ər.əl/",
  "definition": "Lasting for a very short time; transitory."
}
```

`ipa` is optional. `pos` accepts `noun`, `verb`, `adjective`, or `adverb`.

A 20-word sample is included. Replace it with your own.

> Want a larger list? Run `generator/build_database.py` locally. It pulls from the kaikki.org Wiktionary dump and filters by seed words you define.

**3. Enable GitHub Actions**

Settings > Actions > General > Allow all actions.

**4. Create a Personal Access Token**

Settings > Developer settings > Personal access tokens > Fine-grained tokens

Required permissions on your repo: Contents (read/write), Actions (read).

**5. Build the app**

```bash
cd android-app
./gradlew assembleDebug
```

Requires Android 8.0+ (API 26).

**6. Configure the app**

Enter your GitHub username, repo name, and token. Tap Save. The app syncs immediately and then daily.

## Customisation

Card count: change `count=5` in `generate.py`.

Schedule: edit the cron expression in `.github/workflows/daily.yml`.

Card appearance: edit the color constants in `generate.py`.

## License

The code and data in this repository are licensed separately:

* **Code:** MIT. See [LICENSE](LICENSE).
* **Data:** The `words.json` file (and any data generated via `build_database.py`) contains definitions derived from Wiktionary via [kaikki.org](https://kaikki.org). This data is strictly governed by the copyleft [CC BY-SA 3.0](https://creativecommons.org) and [GFDL](https://gnu.org) licenses, and cannot be distributed under the MIT license.
