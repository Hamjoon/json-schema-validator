{
    "newTestCases": [
        {
            "group": "walk – validation enabled (true) – missing required property",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a ValidationMessage indicating that required property \"name\" is missing.",
                    "description": "The schema declares \"name\" as required. Supplying an empty object should cause the required‑property validator to emit a message when validation is enabled."
                }
            ]
        },
        {
            "group": "walk – validation enabled (true) – additionalProperties false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"name\": \"John\", \"age\": 30 }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a ValidationMessage that property \"age\" is not allowed (additionalProperties = false).",
                    "description": "The schema defines only the property \"name\" and sets \"additionalProperties\": false. An extra property should be flagged by the additional‑properties validator."
                }
            ]
        },
        {
            "group": "walk – validation enabled (true) – enum violation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"name\": \"John\", \"status\": \"unknown\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a ValidationMessage that value \"unknown\" is not one of the allowed enum values [\"active\",\"inactive\"].",
                    "description": "The schema adds an enum constraint on property \"status\". Providing a value outside the enum should produce an enum‑validation error."
                }
            ]
        },
        {
            "group": "walk – validation enabled (true) – pattern mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"name\": \"John\", \"email\": \"not-an-email\" }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a ValidationMessage that string \"not-an-email\" does not match the regex pattern \"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\" for property \"email\".",
                    "description": "The schema defines a regex pattern for an email address. Supplying a non‑matching string should trigger the pattern validator."
                }
            ]
        },
        {
            "group": "walk – validation enabled (true) – numeric range violation (minimum)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"age\": -5 }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a ValidationMessage that numeric value -5 is less than the allowed minimum 0 for property \"age\".",
                    "description": "The schema specifies a minimum of 0 for the numeric property \"age\". Providing a negative number should be reported by the numeric‑range validator."
                }
            ]
        },
        {
            "group": "walk – validation enabled (true) – array length violation (minItems)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"tags\": [] }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a ValidationMessage that array \"tags\" has fewer items (0) than the required minimum of 1.",
                    "description": "The schema defines \"tags\" as an array with \"minItems\": 1. An empty array should be flagged by the array‑size validator."
                }
            ]
        },
        {
            "group": "walk – $ref to external schema – valid instance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"address\": { \"street\": \"Main St\", \"zip\": \"12345\" } }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "An empty Set – the referenced address schema validates the supplied address object successfully.",
                    "description": "The main schema contains a \"$ref\": \"address-schema.json#\". The provided address object conforms to the external schema, so no validation messages are expected."
                }
            ]
        },
        {
            "group": "walk – $ref to external schema – invalid instance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"address\": { \"street\": 100, \"zip\": \"12\" } }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing at least two ValidationMessages: one for type mismatch on \"street\" (expected string, found integer) and one for pattern/minLength violation on \"zip\" (value \"12\" does not satisfy the zip‑code pattern).",
                    "description": "The referenced external schema expects \"street\" to be a string and \"zip\" to match a five‑digit pattern. Supplying an integer and a too‑short zip should generate appropriate messages."
                }
            ]
        },
        {
            "group": "walk – $ref with URL‑encoded fragment",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "{ \"self\": { \"$ref\": \"#/properties%2Fname\" } }"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "An empty Set – the URL‑decoded fragment resolves to the \"name\" property schema, which validates the supplied string value.",
                    "description": "The $ref contains an URL‑encoded JSON‑Pointer. The walk method must decode it correctly before locating the target schema."
                }
            ]
        },
        {
            "group": "walk – boolean schema true (accept any instance)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "12345"
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "An empty Set – a schema defined as the boolean literal true accepts any JSON value without producing messages.",
                    "description": "When the root schema is the boolean true, walk should short‑circuit validation and return an empty result irrespective of the instance."
                }
            ]
        },
        {
            "group": "walk – boolean schema false (reject any instance)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "\"any value\""
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a single ValidationMessage indicating that the instance is invalid because the schema is false.",
                    "description": "A schema defined as the boolean false must cause every instance to be considered invalid, resulting in a generic validation error."
                }
            ]
        },
        {
            "group": "walk – instance type mismatch (scalar vs object)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "\"just a string\""
                        },
                        {
                            "parameter": "outputFormat",
                            "value": "CollectingOutputFormat<Set<ValidationMessage>>"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "A Set containing a ValidationMessage that the instance type is STRING but the schema expects OBJECT.",
                    "description": "The schema is defined for an object with required property \"name\". Supplying a plain string should trigger the type validator."
                }
            ]
        }
    ]
}