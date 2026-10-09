#!/usr/bin/env python3
"""
Optional helper: build words.json from the kaikki.org English Wiktionary dump.

This is one way to source a word list. You can also curate words.json manually
or use any other data source, see the schema at the bottom of this file.

Run:   python generator/build_database.py
Output: words.json (review before use)

The kaikki.org dump is ~3 GB. The script streams it and never writes it to disk.
"""

import json
import re
import sys
import urllib.request
from pathlib import Path

JSONL_URL = "https://kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl"
OUTPUT = Path(__file__).parent.parent / "words.json"

# ── Customize: add seed words that match your desired vocabulary theme ────────
# These anchor the topic; the script keeps entries whose definitions share
# at least one word with this set, giving you a thematically consistent list.
SEED_WORDS: set[str] = {
    # Example: philosophical / reflective vocabulary
    "wisdom", "virtue", "courage", "patience", "justice", "prudence",
    "temperance", "integrity", "resilience", "contemplation", "equanimity",
    "fortitude", "serenity", "discipline", "perseverance", "magnanimity",
    # Add your own seed words here
}

TARGET_POS = {"noun", "verb", "adjective", "adverb"}
MIN_WORD_LEN = 4
MAX_WORD_LEN = 20

IPA_RE = re.compile(r"/[^/]+/")


def extract_ipa(entry: dict) -> str:
    for sound in entry.get("sounds", []):
        if "ipa" in sound:
            return sound["ipa"]
    return ""


def extract_definition(entry: dict) -> str:
    for sense in entry.get("senses", []):
        for gloss in sense.get("glosses", []):
            text = gloss.strip()
            if text and len(text) > 20:
                return text
    return ""


def is_candidate(word: str, pos: str, definition: str) -> bool:
    if not definition or pos not in TARGET_POS:
        return False
    if not (MIN_WORD_LEN <= len(word) <= MAX_WORD_LEN):
        return False
    if not re.match(r"^[a-z][a-z\-]+$", word):
        return False
    def_words = set(re.findall(r"[a-z]+", definition.lower()))
    return bool(def_words & SEED_WORDS)


def build(limit: int | None = None):
    print(f"Streaming {JSONL_URL} …")
    candidates: list[dict] = []

    with urllib.request.urlopen(JSONL_URL) as resp:
        for raw_line in resp:
            line = raw_line.decode("utf-8").strip()
            if not line:
                continue
            try:
                entry = json.loads(line)
            except json.JSONDecodeError:
                continue

            word = entry.get("word", "").lower()
            pos = entry.get("pos", "")
            definition = extract_definition(entry)

            if not is_candidate(word, pos, definition):
                continue

            candidates.append({
                "word": word,
                "pos": pos,
                "ipa": extract_ipa(entry),
                "definition": definition,
            })

            if limit and len(candidates) >= limit:
                break

            if len(candidates) % 500 == 0:
                print(f"  {len(candidates)} candidates so far…", end="\r", flush=True)

    # Deduplicate by word (keep first occurrence)
    seen: set[str] = set()
    unique = []
    for c in candidates:
        if c["word"] not in seen:
            seen.add(c["word"])
            unique.append(c)

    unique.sort(key=lambda e: e["word"])
    OUTPUT.write_text(json.dumps(unique, indent=2, ensure_ascii=False))
    print(f"\n✓ Wrote {len(unique)} entries → {OUTPUT}")


# ── words.json schema ─────────────────────────────────────────────────────────
# words.json is a JSON array of objects. Each object must have:
#
#   "word":       string  the word (lowercase)
#   "pos":        string  part of speech: "noun", "verb", "adjective", "adverb"
#   "ipa":        string  IPA pronunciation, e.g. "/ɪˈfɛm.ər.əl/" (may be empty)
#   "definition": string  one-sentence plain-text definition
#
# Example:
# [
#   {
#     "word": "ephemeral",
#     "pos": "adjective",
#     "ipa": "/ɪˈfɛm.ər.əl/",
#     "definition": "Lasting for a very short time; transitory."
#   }
# ]
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else None
    build(limit)
