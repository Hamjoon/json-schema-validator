{
    "group": "BaseJsonValidator#getKeyword() - normal and edge cases",
    "cases": [
        {
            "input": [],
            "expected": "\"type\"",
            "description": "When the internal `keyword` field is a non‑null object whose `getValue()` returns the string \"type\", `getKeyword()` should return exactly that string."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "If the internal `keyword` field is `null`, invoking `getKeyword()` should throw a `NullPointerException` because the method dereferences `keyword` without a null‑check."
        },
        {
            "input": [],
            "expected": "null",
            "description": "When the internal `keyword` object is non‑null but its `getValue()` method returns `null`, `getKeyword()` should propagate that `null` value as its result."
        }
    ]
}