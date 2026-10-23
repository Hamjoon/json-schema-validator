{
    "group": "getOutermostSchema_exceptions",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "The internal call getDynamicScope() returns null (e.g., the dynamic scope stack is empty). The subsequent call to getContainingSchema() on a null reference triggers a NullPointerException."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "The current dynamic scope's containing schema is non‑null, but its findLexicalRoot() method returns null. When the code attempts lexicalRoot.isDynamicAnchor(), a NullPointerException is thrown."
        }
    ]
}