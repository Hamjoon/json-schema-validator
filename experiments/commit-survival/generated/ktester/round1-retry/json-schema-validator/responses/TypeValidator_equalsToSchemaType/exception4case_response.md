{
    "testCases": [
        {
            "group": "Null validationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "JsonNode representing integer 10"
                        },
                        {
                            "parameter": "schemaType",
                            "value": "JsonType.INTEGER"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        },
                        {
                            "parameter": "validationContext",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The ValidationContext argument is null; equalsToSchemaType should propagate a NullPointerException because the delegate method expects a non‑null context."
                }
            ]
        },
        {
            "group": "Null node reference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "null (Java null reference, not Jackson NullNode)"
                        },
                        {
                            "parameter": "schemaType",
                            "value": "JsonType.STRING"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        },
                        {
                            "parameter": "validationContext",
                            "value": "default ValidationContext"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a literal null for the JsonNode argument (instead of a Jackson NullNode) should cause a NullPointerException inside JsonNodeUtil.equalsToSchemaType."
                }
            ]
        },
        {
            "group": "Invalid schema type enum",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "JsonNode representing string \"test\""
                        },
                        {
                            "parameter": "schemaType",
                            "value": "JsonType.UNKNOWN (non‑existent enum constant)"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        },
                        {
                            "parameter": "validationContext",
                            "value": "default ValidationContext"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Supplying a schema type that is not part of the JsonType enum should cause the utility method to throw IllegalArgumentException because it cannot map the type to a known JSON primitive."
                }
            ]
        },
        {
            "group": "Malformed parent schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "node",
                            "value": "JsonNode representing integer 5"
                        },
                        {
                            "parameter": "schemaType",
                            "value": "JsonType.INTEGER"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "JsonSchema whose declared type is a non‑JsonType value (e.g., \"invalid-type\")"
                        },
                        {
                            "parameter": "validationContext",
                            "value": "default ValidationContext"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When the parent schema contains an invalid or unrecognisable type definition, JsonNodeUtil.equalsToSchemaType should raise IllegalArgumentException while trying to resolve the parent type."
                }
            ]
        }
    ]
}