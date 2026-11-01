{
    "test_cases": [
        {
            "group": "Invalid $ref syntax – exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"$ref\":\"invalid#ref\"}"
                        },
                        {
                            "parameter": "format",
                            "value": "CollectingOutputFormat"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because the $ref value cannot be parsed as a valid JSON Pointer",
                    "description": "The schema contains a $ref with an illegal fragment. The validate method should attempt to resolve the reference, detect the malformed pointer, and throw an IllegalArgumentException rather than silently ignoring it."
                }
            ]
        },
        {
            "group": "Custom validator throws unchecked exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"trigger\":\"error\"}"
                        },
                        {
                            "parameter": "format",
                            "value": "CollectingOutputFormat"
                        }
                    ],
                    "expected": "RuntimeException (or subclass) propagated out of validate",
                    "description": "A schema is configured with a user‑defined JsonValidator that deliberately throws a RuntimeException when its validate method is invoked. The validate(JsonNode, OutputFormat) method must not catch this exception; it should bubble up to the caller."
                }
            ]
        },
        {
            "group": "ExecutionContextCustomizer throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"name\":\"John\"}"
                        },
                        {
                            "parameter": "format",
                            "value": "CollectingOutputFormat"
                        }
                    ],
                    "expected": "RuntimeException thrown from the ExecutionContextCustomizer and propagated by validate",
                    "description": "When the three‑argument overload is invoked via the public validate method, a null customizer is normally used. This test forces the path where a non‑null ExecutionContextCustomizer is supplied and its accept method throws an exception. The exception must propagate unchanged."
                }
            ]
        },
        {
            "group": "OutputFormat throws IOException during message addition",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"age\":30}"
                        },
                        {
                            "parameter": "format",
                            "value": "IOExceptionThrowingOutputFormat (throws IOException on addMessage)"
                        }
                    ],
                    "expected": "IOException propagated out of validate",
                    "description": "A custom OutputFormat implementation throws an IOException when the validator tries to add a ValidationMessage. The validate method should not swallow the checked exception; it must be declared or wrapped and re‑thrown."
                }
            ]
        },
        {
            "group": "Circular $ref without $recursiveAnchor leads to recursion limit exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"$ref\":\"#/definitions/self\"}"
                        },
                        {
                            "parameter": "format",
                            "value": "CollectingOutputFormat"
                        }
                    ],
                    "expected": "IllegalStateException (or JsonSchemaException) indicating recursion depth exceeded",
                    "description": "The schema defines a $ref that points to itself and does not declare a $recursiveAnchor. During validation the implementation should detect the potential infinite loop and abort with a clear exception rather than causing a StackOverflowError."
                }
            ]
        }
    ]
}