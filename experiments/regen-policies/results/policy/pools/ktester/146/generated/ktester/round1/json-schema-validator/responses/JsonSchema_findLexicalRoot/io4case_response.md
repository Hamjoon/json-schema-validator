{
    "group": "findLexicalRoot",
    "cases": [
        {
            "input": [],
            "expected": "the same JsonSchema instance that was called (its getId() returns an empty string, which is non‑null)",
            "description": "The schema’s ID is an empty string (\"\"), which is not null. The while‑loop condition fails immediately, so the method should return the original instance."
        },
        {
            "input": [],
            "expected": "the direct parent JsonSchema instance (parent’s getId() returns an empty string)",
            "description": "The schema’s ID is null, but its immediate parent has an ID that is an empty string. The loop iterates once, moves to the parent, sees a non‑null ID (even though it is empty), exits, and returns that parent."
        },
        {
            "input": [],
            "expected": "the grand‑parent JsonSchema instance (grand‑parent’s getId() returns an empty string)",
            "description": "The schema’s ID is null, its parent’s ID is also null, but the grand‑parent’s ID is an empty string. The loop climbs two levels, finds the first ancestor with a non‑null ID (the empty string), and returns that grand‑parent."
        },
        {
            "input": [],
            "expected": "the same JsonSchema instance that was called (its getId() returns a whitespace string, which is non‑null)",
            "description": "The schema’s ID is a string containing only whitespace (e.g., \"   \"). Since the method only checks for null, the whitespace string is treated as a valid ID, causing the loop to terminate immediately and return the original instance."
        },
        {
            "input": [],
            "expected": "the direct parent JsonSchema instance (parent’s getId() returns a whitespace string)",
            "description": "The schema’s ID is null, but its parent’s ID is a whitespace‑only string. The loop moves to the parent, detects a non‑null ID, and returns the parent."
        }
    ]
}