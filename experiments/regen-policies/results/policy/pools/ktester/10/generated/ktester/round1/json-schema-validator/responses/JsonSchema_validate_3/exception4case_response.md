{
    "groups": [
        {
            "group": "Malformed $ref Handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"refField\":\"value\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchemaException with message containing 'Unable to resolve $ref'",
                    "description": "The schema contains a $ref with an illegal URI encoding (e.g., \"%ZZ\") that triggers URLDecoder to throw IllegalArgumentException. The validate method should surface this as a JsonSchemaException."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"refField\":\"value\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Invalid $ref'",
                    "description": "The $ref points to a non‑existent definition within the schema. During validation the resolver throws IllegalArgumentException, which must be propagated by the validate method."
                }
            ]
        },
        {
            "group": "Invalid Schema Definition",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"name\":\"John\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Invalid type keyword'",
                    "description": "The compiled schema contains an unsupported value for the \"type\" keyword (e.g., \"unknownType\"). When validate is called the schema parser throws IllegalArgumentException, which should be propagated."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"age\":30}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonProcessingException",
                    "description": "The schema definition includes a malformed JSON fragment (e.g., missing a closing brace). Loading the schema triggers JsonProcessingException, which the validate method must not swallow."
                }
            ]
        },
        {
            "group": "ExecutionCustomizer Throws Error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"name\":\"John\",\"age\":30}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "lambda that throws AssertionError(\"customizer assertion failure\")"
                        }
                    ],
                    "expected": "AssertionError with message 'customizer assertion failure'",
                    "description": "When the consumer throws an Error (not a RuntimeException), validate should propagate it unchanged."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"name\":\"John\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "lambda that sets ExecutionContext.setValidateSchema(true) then throws NullPointerException"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Even if the customizer mutates the ExecutionContext before failing, the original NullPointerException must be propagated."
                }
            ]
        }
    ]
}