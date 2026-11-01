{
    "testCases": [
        {
            "group": "preloadJsonSchemas - iterator exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Custom Collection whose iterator() method throws IllegalStateException"
                        }
                    ],
                    "expected": "IllegalStateException is propagated out of preloadJsonSchemas",
                    "description": "Verifies that if obtaining the iterator fails, the exception bubbles up and the method does not catch it."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - hasNext exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Custom Collection with iterator that throws RuntimeException when hasNext() is called"
                        }
                    ],
                    "expected": "RuntimeException is propagated out of preloadJsonSchemas",
                    "description": "Ensures that an exception thrown during the loop's hasNext() check is not swallowed."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - next exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "Custom Collection with iterator that throws IllegalArgumentException when next() is called"
                        }
                    ],
                    "expected": "IllegalArgumentException is propagated out of preloadJsonSchemas",
                    "description": "Checks that an exception thrown while retrieving the next element aborts the method and propagates."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - initializeValidators throws Error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List containing one mock JsonSchema whose initializeValidators() throws AssertionError"
                        }
                    ],
                    "expected": "AssertionError is propagated out of preloadJsonSchemas",
                    "description": "Confirms that Errors (not just Exceptions) thrown by initializeValidators are not caught."
                }
            ]
        },
        {
            "group": "preloadJsonSchemas - later schema throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemas",
                            "value": "List with two mock JsonSchema objects; first.initializeValidators() succeeds, second.initializeValidators() throws IllegalStateException"
                        }
                    ],
                    "expected": "IllegalStateException is propagated after the first schema has been processed; the method does not continue to further schemas",
                    "description": "Validates that processing stops as soon as a later schema throws, and the exception propagates."
                }
            ]
        }
    ]
}