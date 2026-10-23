{
    "group": "CollectorContext.getDynamicScope - empty stack",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When no dynamic scopes have been entered, the internal Deque is empty. Calling getDynamicScope() should return null (Deque.peek() returns null on an empty collection)."
        }
    ]
}