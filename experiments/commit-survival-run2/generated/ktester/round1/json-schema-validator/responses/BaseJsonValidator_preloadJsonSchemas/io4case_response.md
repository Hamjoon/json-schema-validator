{
    "tests": [
        {
            "group": "preloadJsonSchemas - duplicate schema instances",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List containing the same mock JsonSchema instance 'schemaX' three times"
                        }
                    ],
                    "expected": "initializeValidators() is invoked exactly three times on schemaX; method returns normally without exception",
                    "description": "Ensures that the method does not deduplicate elements and calls initializeValidators() for each occurrence, even when the same object appears multiple times in the collection."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - unmodifiable collection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Collections.unmodifiableList of two mock JsonSchema objects (schemaU1, schemaU2) where initializeValidators() does nothing"
                        }
                    ],
                    "expected": "initializeValidators() is invoked once on schemaU1 and once on schemaU2; method returns normally without exception",
                    "description": "Verifies that the method works with an unmodifiable collection, i.e., it does not attempt to modify the collection while iterating."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - iterator throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Custom Collection whose iterator.next() throws IllegalStateException(\"iterator failure\") after returning the first mock schema"
                        }
                    ],
                    "expected": "IllegalStateException with message \"iterator failure\" propagates out of preloadJsonSchemas",
                    "description": "Confirms that any exception thrown by the iterator during traversal is not caught by the method and propagates to the caller."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - mixed null and valid schemas",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List containing a mock JsonSchema (schemaM) followed by a null element"
                        }
                    ],
                    "expected": "NullPointerException is thrown when attempting to invoke initializeValidators() on the null element",
                    "description": "Checks that the method stops processing as soon as it encounters a null entry, mirroring the behaviour of a plain for‑each loop, even when preceding elements are valid."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - side‑effect verification",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List of two mock JsonSchema objects (schemaS1, schemaS2) whose initializeValidators() increments a shared AtomicInteger counter"
                        }
                    ],
                    "expected": "After method execution the counter value is 2, indicating that initializeValidators() was called once for each schema; method returns normally",
                    "description": "Uses a side‑effect (incrementing a counter) to verify that initializeValidators() is indeed invoked for every schema in the collection."
                }
            ]
        }
    ]
}