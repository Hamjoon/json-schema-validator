{
    "test_cases": [
        {
            "group": "ExecutionContext Creation Failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "new CollectingFormat<>()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "A subclass of JsonSchema overrides createExecutionContext() to throw a RuntimeException. The validate method should propagate this exception without catching it."
                }
            ]
        },
        {
            "group": "Malformed $ref Handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "new CollectingFormat<>()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The schema contains a $ref with an illegal URI fragment (e.g., \"#/definitions/%ZZ\"). URLDecoder.decode will throw IllegalArgumentException, which must propagate out of validate()."
                }
            ]
        },
        {
            "group": "Null Validators List",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "new CollectingFormat<>()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "A JsonSchema subclass overrides getValidators() to return null. When validate() iterates over the validator list it will encounter a NullPointerException, which should be propagated."
                }
            ]
        },
        {
            "group": "OutputFormat Throws Error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<String>() { @Override public String format(Set<ValidationMessage> messages, ExecutionContext ctx) { throw new AssertionError(\"format error\"); } }"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "AssertionError",
                    "description": "When the OutputFormat implementation throws an Error (AssertionError), validate() must not catch it and should let it propagate to the caller."
                }
            ]
        },
        {
            "group": "UnsupportedEncodingException from $ref Decoding",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "validJsonNode"
                        },
                        {
                            "parameter": "format",
                            "value": "new CollectingFormat<>()"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "ctx -> {}"
                        }
                    ],
                    "expected": "UnsupportedEncodingException",
                    "description": "A $ref contains a percent‑encoded sequence using an unsupported charset (e.g., \"%C3%28\"). URLDecoder.decode may throw UnsupportedEncodingException, which should be propagated by validate()."
                }
            ]
        }
    ]
}