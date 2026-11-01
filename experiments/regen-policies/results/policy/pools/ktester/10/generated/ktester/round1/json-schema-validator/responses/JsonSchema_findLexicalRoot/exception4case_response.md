{
    "group": "findLexicalRoot exception scenarios",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "Invoking findLexicalRoot() on a null JsonSchema reference should throw NullPointerException before any method logic is executed."
        },
        {
            "input": [],
            "expected": "Infinite loop (test timeout)",
            "description": "If a JsonSchema instance has a parent that references the instance itself (cyclic reference) and both have null ids, the while loop never encounters a non‑null id or a null parent, resulting in an infinite loop. The test should verify that the method does not terminate (e.g., by enforcing a timeout)."
        }
    ]
}