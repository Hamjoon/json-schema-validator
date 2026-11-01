{
    "tests": [
        {
            "group": "JSON null node handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "JsonNode representing JSON literal null"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true and getMessages().isEmpty() (schema type includes \"null\")",
                    "description": "When the instance is a JSON null value and the schema permits the null type, walk should consider the instance valid."
                },
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "JsonNode representing JSON literal null"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and contains a message about type mismatch (schema does not allow null)",
                    "description": "When the instance is a JSON null value but the schema does not list \"null\" as an allowed type, walk must report a type‑mismatch error."
                }
            ]
        },
        {
            "group": "Missing required property",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "ObjectNode representing {\"age\":30}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and contains a required‑property message for \"name\"",
                    "description": "The schema declares \"name\" as required; the instance omits it, so walk should return an invalid result with a required‑property error."
                }
            ]
        },
        {
            "group": "Array constraints – minItems",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "ArrayNode representing [1]"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and contains a message about failing minItems (expected >=2)",
                    "description": "Schema defines an array with minItems = 2; the instance contains only one element, therefore walk must flag the minItems violation."
                }
            ]
        },
        {
            "group": "String pattern validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "TextNode representing \"abc123\""
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and contains a pattern‑mismatch message",
                    "description": "The schema requires the string to match the regex ^[A-Z]+$; the supplied lower‑case alphanumeric string does not match, so walk should report a pattern error."
                }
            ]
        },
        {
            "group": "$ref resolution success",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "ObjectNode representing {\"$ref\":\"#/definitions/person\"}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true and getMessages().isEmpty() (the referenced definition validates the instance)",
                    "description": "The instance contains a $ref that points to a valid definition within the same schema; walk must resolve the reference and validate successfully."
                }
            ]
        },
        {
            "group": "AdditionalProperties false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "ObjectNode representing {\"name\":\"John\",\"age\":30,\"extra\":\"value\"}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and contains a message about disallowed additional property \"extra\"",
                    "description": "Schema sets additionalProperties = false; the instance includes an unexpected property, so walk should report an additional‑property violation."
                }
            ]
        },
        {
            "group": "Numeric range violation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "NumericNode representing -5"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and contains a message about value being less than the minimum (0)",
                    "description": "Schema defines a minimum of 0 for the numeric field; the instance provides -5, causing a range‑error that walk must surface."
                }
            ]
        },
        {
            "group": "Conditional validation – then branch passes",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "ObjectNode representing {\"type\":\"A\",\"value\":10}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == true and getMessages().isEmpty() (if condition matches and then schema validates)",
                    "description": "Schema uses if/then/else where if \"type\" == \"A\" then \"value\" must be an integer >=0. The instance satisfies both, so walk returns a valid result."
                }
            ]
        },
        {
            "group": "Conditional validation – else branch violation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "ObjectNode representing {\"type\":\"B\",\"value\":\"oops\"}"
                        },
                        {
                            "parameter": "validate",
                            "value": true
                        }
                    ],
                    "expected": "ValidationResult.isValid() == false and contains a message about type mismatch for \"value\" in the else schema",
                    "description": "The instance does not meet the if condition, so the else schema applies, which expects \"value\" to be a number. The provided string triggers a validation error."
                }
            ]
        }
    ]
}