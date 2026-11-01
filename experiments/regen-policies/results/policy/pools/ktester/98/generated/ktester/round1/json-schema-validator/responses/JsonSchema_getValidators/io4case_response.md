{
    "group": "getValidators - empty validators list",
    "cases": [
        {
            "input": [],
            "expected": "Returns an unmodifiable empty list when the internal `read` method yields an empty mutable list. The list size is 0 and attempts to add elements throw UnsupportedOperationException.",
            "description": "A JsonSchema spy is configured so that `read(getSchemaNode())` returns `new ArrayList<>()`. The first call to `getValidators()` should store and return an unmodifiable view of this empty list. Verifies that the list is empty and immutable."
        }
    ]
}