{
    "groups": [
        {
            "group": "JsonSchema#getId - special characters",
            "cases": [
                {
                    "input": [],
                    "expected": "!@#$%^&*()_+-=[]{}|;':,.<>?/~`",
                    "description": "Verify that getId() returns an id containing a full range of punctuation and special symbols without alteration."
                },
                {
                    "input": [],
                    "expected": "id-with-percent%20encoding",
                    "description": "Verify that getId() correctly returns an id that includes percent‑encoded characters (e.g., space encoded as %20)."
                }
            ]
        },
        {
            "group": "JsonSchema#getId - whitespace handling",
            "cases": [
                {
                    "input": [],
                    "expected": "  leadingSpace",
                    "description": "Confirm that leading whitespace characters are preserved in the returned id."
                },
                {
                    "input": [],
                    "expected": "trailingSpace  ",
                    "description": "Confirm that trailing whitespace characters are preserved in the returned id."
                },
                {
                    "input": [],
                    "expected": "  both  ",
                    "description": "Confirm that both leading and trailing whitespace are preserved."
                }
            ]
        },
        {
            "group": "JsonSchema#getId - Unicode characters",
            "cases": [
                {
                    "input": [],
                    "expected": "スキーマID",
                    "description": "Validate that getId() returns an id containing Japanese characters without corruption."
                },
                {
                    "input": [],
                    "expected": "emoji-🚀-test",
                    "description": "Validate that getId() returns an id containing emoji characters correctly."
                },
                {
                    "input": [],
                    "expected": "café-ñ",
                    "description": "Validate that getId() returns an id with accented Latin characters and diacritics."
                }
            ]
        },
        {
            "group": "JsonSchema#getId - after other method calls",
            "cases": [
                {
                    "input": [],
                    "expected": "post-toString-id",
                    "description": "Call toString() on the JsonSchema instance first, then call getId(); ensure the id remains unchanged."
                },
                {
                    "input": [],
                    "expected": "post-walk-id",
                    "description": "Invoke walk(JsonNode) on the schema (or any other non‑mutating method) before getId(); verify the id is still the original value."
                }
            ]
        },
        {
            "group": "JsonSchema#getId - concurrent access",
            "cases": [
                {
                    "input": [],
                    "expected": "concurrent-id",
                    "description": "Spawn multiple threads that simultaneously invoke getId() on the same JsonSchema instance; all should receive the identical id value, demonstrating thread‑safety of the getter."
                }
            ]
        }
    ]
}