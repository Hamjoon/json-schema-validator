{
    "testCases": [
        {
            "group": "Null internal schemas list",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException is thrown by preloadJsonSchema()",
                    "description": "A subclass of AnyOfValidator sets the internal `schemas` field to null (e.g., via reflection) before invoking preloadJsonSchema(). Since the method dereferences `this.schemas`, a NullPointerException should be thrown."
                }
            ]
        },
        {
            "group": "Unmodifiable schemas list",
            "cases": [
                {
                    "input": [],
                    "expected": "UnsupportedOperationException is thrown by preloadJsonSchema()",
                    "description": "A subclass replaces the internal `schemas` list with `Collections.unmodifiableList(new ArrayList<>())`. When preloadJsonSchema() forwards this list to `preloadJsonSchemas`, any attempt to modify the list results in UnsupportedOperationException."
                }
            ]
        },
        {
            "group": "Specific unchecked exception propagation",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalArgumentException is thrown by preloadJsonSchema()",
                    "description": "A subclass overrides `preloadJsonSchemas` to throw an IllegalArgumentException. The test verifies that this specific unchecked exception propagates unchanged to the caller."
                }
            ]
        }
    ]
}