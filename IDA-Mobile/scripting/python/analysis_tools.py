from __future__ import annotations

import collections
import json
import math
import re
from typing import Iterable


URL_RE = re.compile(r"https?://[^\s\"'>]+", re.IGNORECASE)
IPV4_RE = re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b")
EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
ANDROID_KEY_RE = re.compile(r"\bAIza[0-9A-Za-z\-_]{35}\b")
HEX_LONG_RE = re.compile(r"\b[0-9a-fA-F]{32,}\b")
BASE64ISH_RE = re.compile(r"\b[A-Za-z0-9+/]{24,}={0,2}\b")


def _entropy(value: str) -> float:
    if not value:
        return 0.0
    counts = collections.Counter(value)
    total = len(value)
    return -sum((count / total) * math.log2(count / total) for count in counts.values())


def _score_string(value: str) -> tuple[float, list[str]]:
    score = 0.0
    reasons: list[str] = []
    entropy = _entropy(value)

    if URL_RE.search(value):
        score += 30.0
        reasons.append("url")
    if IPV4_RE.search(value):
        score += 15.0
        reasons.append("ip")
    if EMAIL_RE.search(value):
        score += 12.0
        reasons.append("email")
    if ANDROID_KEY_RE.search(value):
        score += 50.0
        reasons.append("google_api_key_pattern")
    if HEX_LONG_RE.search(value):
        score += 14.0
        reasons.append("long_hex_token")
    if BASE64ISH_RE.search(value):
        score += 10.0
        reasons.append("base64_like")
    if len(value) >= 32:
        score += 6.0
        reasons.append("long_string")
    if entropy >= 4.2 and len(value) >= 16:
        score += min(22.0, (entropy - 4.2) * 10.0)
        reasons.append("high_entropy")

    lower = value.lower()
    if any(token in lower for token in ("token", "secret", "apikey", "bearer", "auth", "password")):
        score += 20.0
        reasons.append("credential_keyword")

    return score, reasons


def rank_strings_json(strings: Iterable[str], limit: int = 200) -> str:
    """
    Return ranked string insights as a JSON array

    Output schema:
    [
      {
        "value": "...",
        "score": 42.5,
        "reasons": ["url", "high_entropy"]
      }
    ]
    """
    unique = []
    seen = set()
    for raw in strings:
        value = str(raw)
        if not value:
            continue
        if value in seen:
            continue
        seen.add(value)
        unique.append(value)

    results = []
    for value in unique:
        score, reasons = _score_string(value)
        if score <= 0:
            continue
        results.append(
            {
                "value": value,
                "score": round(score, 2),
                "reasons": reasons,
            }
        )

    results.sort(key=lambda item: (item["score"], len(item["value"])), reverse=True)
    return json.dumps(results[: max(1, int(limit))], ensure_ascii=False)

