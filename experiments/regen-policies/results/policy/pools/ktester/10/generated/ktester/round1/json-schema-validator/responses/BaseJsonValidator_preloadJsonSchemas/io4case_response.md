{
    "group": "preloadJsonSchemas edge cases",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "[mockSchema1, null, mockSchema2]"
                }
            ],
            "expected": "NullPointerException is thrown when the method attempts to invoke initializeValidators() on the null element; no further schemas are processed.",
            "description": "Validate that a collection containing a null entry causes a NPE during iteration, confirming the method does not guard against null elements."
        },
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "[mockSchemaDup, mockSchemaDup]"
                }
            ],
            "expected": "initializeValidators() is invoked exactly twice on the same mockSchemaDup instance; method returns normally.",
            "description": "Ensure that duplicate references in the collection are each processed independently, i.e., the method does not deduplicate or skip repeated schemas."
        },
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "[mockSchemaError]"
                }
            ],
            "expected": "Error (e.g., AssertionError) thrown by mockSchemaError.initializeValidators() propagates out of preloadJsonSchemas.",
            "description": "Confirm that non‑RuntimeException throwables (such as java.lang.Error) are not caught and bubble up unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "[mockSchemaGood, mockSchemaThrows, mockSchemaLater]"
                }
            ],
            "expected": "initializeValidators() is called on mockSchemaGood; then RuntimeException from mockSchemaThrows propagates; initializeValidators() on mockSchemaLater is never invoked.",
            "description": "Test that iteration stops at the first exception: earlier schemas are processed, the exception is propagated, and later schemas are not touched."
        },
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "new HashSet<>(Arrays.asList(mockSchemaA, mockSchemaB))"
                }
            ],
            "expected": "initializeValidators() is invoked once on each of mockSchemaA and mockSchemaB; method returns normally.",
            "description": "Verify that the method works with a Set implementation, confirming that order does not affect the invocation of initializeValidators on each element."
        }
    ]
}