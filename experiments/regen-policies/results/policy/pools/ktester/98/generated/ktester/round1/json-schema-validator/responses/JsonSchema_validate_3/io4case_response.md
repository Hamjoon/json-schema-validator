{
    "testCases": [
        {
            "group": "Root node type mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ArrayNode representing [1,2,3]"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "Set containing ValidationMessage with type 'type' and path ''",
                    "description": "The schema expects an object at the root, but an array is supplied; validation should report a type error at the root path."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "StringNode representing \"just a string\""
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "Set containing ValidationMessage with type 'type' and path ''",
                    "description": "When the root JSON node is a primitive string while the schema requires an object, a type ValidationMessage must be returned."
                }
            ]
        },
        {
            "group": "Pattern validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"email\":\"not-an-email\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "Set containing ValidationMessage with type 'format' and path '/email'",
                    "description": "The schema defines a regex/pattern for the \"email\" property; providing a non‑matching value should yield a format ValidationMessage."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"email\":\"john@example.com\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "empty Set<ValidationMessage>",
                    "description": "A correctly formatted email satisfies the pattern constraint, resulting in no validation messages."
                }
            ]
        },
        {
            "group": "Enum validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"status\":\"UNKNOWN\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "Set containing ValidationMessage with type 'enum' and path '/status'",
                    "description": "The \"status\" property is limited to a set of allowed enum values; an out‑of‑range value must generate an enum ValidationMessage."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"status\":\"ACTIVE\"}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "empty Set<ValidationMessage>",
                    "description": "A value that belongs to the defined enum list passes validation without errors."
                }
            ]
        },
        {
            "group": "OneOf validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"value\":123}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "empty Set<ValidationMessage>",
                    "description": "The schema uses oneOf to allow either a numeric or a string representation; providing a number satisfies one branch, so validation succeeds."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"value\":true}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "Set containing ValidationMessage with type 'oneOf' and path '/value'",
                    "description": "A boolean does not match any of the oneOf alternatives (number or string), therefore a oneOf ValidationMessage should be produced."
                }
            ]
        },
        {
            "group": "Array items validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ArrayNode representing [\"a\", \"b\", \"c\"]"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "empty Set<ValidationMessage>",
                    "description": "The schema requires an array of strings; an array where every element is a string validates without errors."
                },
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ArrayNode representing [\"a\", 2, \"c\"]"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "Set containing ValidationMessage with type 'type' and path '/1'",
                    "description": "The second element is an integer while the schema expects strings; a type ValidationMessage should be reported at the offending index."
                }
            ]
        },
        {
            "group": "Null customizer with non‑empty valid object",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"name\":\"Alice\",\"age\":25}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "null"
                        }
                    ],
                    "expected": "empty Set<ValidationMessage>",
                    "description": "A null executionCustomizer should be treated as a no‑op; a valid object must still produce an empty validation result."
                }
            ]
        },
        {
            "group": "Custom exception propagation (different exception type)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"a\":1}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> { throw new RuntimeException(\"unexpected\"); }"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "If the customizer throws a RuntimeException (not IllegalStateException), the validate method must propagate it unchanged."
                }
            ]
        },
        {
            "group": "Deep nesting exceeding max depth without recursive anchor",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "ObjectNode representing {\"level1\":{\"level2\":{\"level3\":{\"level4\":{\"level5\":{}}}}}}"
                        },
                        {
                            "parameter": "executionCustomizer",
                            "value": "consumer -> {}"
                        }
                    ],
                    "expected": "Set containing ValidationMessage with type 'maxDepth' (or similar)",
                    "description": "When the instance depth exceeds the validator's configured maximum depth and the schema does not use a recursive anchor, validation should stop and report a maxDepth ValidationMessage."
                }
            ]
        }
    ]
}