{
    "new_test_cases": [
        {
            "group": "Null InputFormat",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"{\\\"key\\\":\\\"value\\\"}\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new SimpleStringOutputFormat<>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null InputFormat should cause the internal deserialize method to throw a NullPointerException (or IllegalArgumentException). The walk method must propagate this exception."
                }
            ]
        },
        {
            "group": "Empty String Input",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "new ExecutionContext()"
                        },
                        {
                            "parameter": "input",
                            "value": "\"\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "new SimpleStringOutputFormat<>()"
                        },
                        {
                            "parameter": "validate",
                            "value": "true"
                        }
                    ],
                    "expected": "JsonProcessingException",
                    "description": "An empty string is not a valid JSON document. The deserialize step should fail and throw a JsonProcessingException, which must be propagated by the walk method."
                }
            ]
        }
    ]
}