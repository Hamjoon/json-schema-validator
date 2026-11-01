{
    "testCases": [
        {
            "group": "preloadJsonSchemas - empty collection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Collections.emptyList()"
                        }
                    ],
                    "expected": "No calls to initializeValidators() on any JsonSchema; method returns normally without throwing an exception.",
                    "description": "Ensures that an empty collection is handled gracefully – the loop body is never executed and no side‑effects occur."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - collection containing null element",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List.of(mockSchema, null, anotherMockSchema)"
                        }
                    ],
                    "expected": "NullPointerException is thrown when the null element is accessed.",
                    "description": "Verifies that the method does not guard against null entries; the for‑each iteration attempts to call initializeValidators() on a null reference, resulting in NPE."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - null collection argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown before entering the loop.",
                    "description": "Checks the method's behavior when the caller passes a null collection; the enhanced for‑loop dereferences the argument, causing NPE."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - duplicate schema instances",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List.of(mockSchema, mockSchema)"
                        }
                    ],
                    "expected": "mockSchema.initializeValidators() is invoked exactly twice (once per occurrence); no exception is thrown.",
                    "description": "Confirms that the method does not deduplicate the collection – each element, even if identical, triggers a separate call."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - schema throws during initialization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List.of(faultySchema, healthySchema)"
                        }
                    ],
                    "expected": "RuntimeException (or the specific exception thrown by faultySchema.initializeValidators()) propagates out of preloadJsonSchemas; healthySchema.initializeValidators() may or may not be called depending on iteration order.",
                    "description": "Ensures that the method does not swallow exceptions from a schema's initializeValidators() implementation; the exception should bubble up to the caller."
                }
            ]
        }
    ]
}