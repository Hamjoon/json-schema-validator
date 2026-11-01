{
    "groups": [
        {
            "group": "Enum validation – value not in enum",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "\"RED\""
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"enum\" stating that the value \"RED\" is not one of the allowed values [\"GREEN\",\"BLUE\"].",
                    "description": "The schema defines an enum of [\"GREEN\",\"BLUE\"]. Supplying a string not present in the enum should trigger an enum validation error."
                }
            ]
        },
        {
            "group": "Multiple validation errors – missing required and type mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "123"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing two messages: one with code \"type\" because the instance is a number but an object is expected, and another with code \"required\" mentioning the missing property \"name\" (if the type validator runs before required).",
                    "description": "The schema expects an object with a required string property \"name\". Providing a plain number should cause both a type error and a required‑property error, testing that the validator aggregates multiple failures."
                }
            ]
        },
        {
            "group": "Boolean schema false – always fails",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "\"any\""
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing a single message with code \"false\" (or a generic error) indicating that the schema is boolean false and therefore any instance is invalid.",
                    "description": "When the JsonSchema is created from the literal boolean false, validation must always fail regardless of the instance."
                }
            ]
        },
        {
            "group": "Boolean schema true – always succeeds",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "A schema that is the literal boolean true accepts any JSON value, including null, without producing validation messages."
                }
            ]
        },
        {
            "group": "Pattern validation – string does not match regex",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "\"abc123\""
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"pattern\" indicating that the value \"abc123\" does not match the required regular expression \"^[A-Z]+$\".",
                    "description": "The schema requires the string to consist only of uppercase letters. Supplying a mixed‑case alphanumeric string should trigger a pattern‑validation error."
                }
            ]
        },
        {
            "group": "Number range – value out of bounds",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "150"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"maximum\" stating that the value 150 exceeds the maximum allowed value 100.",
                    "description": "The schema defines a numeric maximum of 100. Providing a number larger than this limit should result in a maximum‑validation error."
                }
            ]
        },
        {
            "group": "Array length – too few items",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "[1,2]"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"minItems\" indicating that the array has 2 items but the minimum required is 3.",
                    "description": "The schema requires an array with at least 3 items. Supplying an array with only 2 items should raise a minItems validation error."
                }
            ]
        },
        {
            "group": "OneOf validation – none match",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "\"maybe\""
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"oneOf\" stating that the instance does not match any of the oneOf subschemas.",
                    "description": "The schema uses a oneOf with two alternatives (e.g., a string \"yes\" or a string \"no\"). Supplying a different string should cause a oneOf validation failure."
                }
            ]
        },
        {
            "group": "AnyOf validation – at least one matches (positive case)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "42"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "The schema defines anyOf with a numeric schema (minimum 0) and a string schema (pattern \"^[a-z]+$\"). Providing a valid number satisfies one of the alternatives, so validation succeeds."
                }
            ]
        },
        {
            "group": "Not validation – value matches prohibited schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "\"forbidden\""
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"not\" indicating that the instance matches a schema that is explicitly prohibited.",
                    "description": "The schema contains a \"not\" clause that disallows the exact string \"forbidden\". Supplying that string should trigger a not‑validation error."
                }
            ]
        },
        {
            "group": "Conditional validation – if true then then clause enforced",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"type\": \"premium\", \"discount\": -5 }"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"minimum\" stating that the \"discount\" value -5 is less than the allowed minimum 0 (as required by the then‑schema).",
                    "description": "The schema uses an if/then construct: if \"type\" equals \"premium\" then \"discount\" must be >= 0. The instance satisfies the if condition but violates the then condition, leading to a minimum‑validation error."
                }
            ]
        },
        {
            "group": "AdditionalProperties false – extra property produces error",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"name\": \"John\", \"age\": 30 }"
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"additionalProperties\" indicating that the property \"age\" is not allowed.",
                    "description": "The schema defines an object with a required property \"name\" and sets \"additionalProperties\": false. Supplying an extra property \"age\" should cause an additionalProperties validation error."
                }
            ]
        },
        {
            "group": "Format validation – invalid email address",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "\"not-an-email\""
                        }
                    ],
                    "expected": "A Set<ValidationMessage> containing one message with code \"format\" stating that the value does not match the email format.",
                    "description": "The schema specifies \"format\": \"email\" for a string property. Providing a string that is not a valid email address should trigger a format‑validation error."
                }
            ]
        },
        {
            "group": "Recursive anchor – self‑referential schema validates correctly",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "{ \"node\": { \"node\": { \"value\": 5 } } }"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "The schema defines a recursive anchor \"$recursiveAnchor\": true and a property \"node\" that references the same schema. A nested structure respecting the schema should validate without stack overflow or errors."
                }
            ]
        },
        {
            "group": "Internal $ref – resolves to definition correctly",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "\"abc123\""
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "The schema contains a $ref to an internal definition that enforces a pattern \"^[a-z]{3}[0-9]{3}$\". The provided string matches the pattern, testing correct resolution of internal $ref."
                }
            ]
        },
        {
            "group": "Relative $ref with $id – resolves base URI correctly",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rootNode",
                            "value": "42"
                        }
                    ],
                    "expected": "An empty Set<ValidationMessage> (no validation errors).",
                    "description": "The schema has an \"$id\": \"http://example.com/schemas/main.json\" and a property that $ref's \"./sub.json\". The referenced sub‑schema allows integers. Supplying an integer checks that relative $ref resolution using the base $id works."
                }
            ]
        }
    ]
}