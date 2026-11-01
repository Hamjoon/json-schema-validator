{
    "groups": [
        {
            "group": "getSchema(String, InputFormat) - whitespace‑only schema string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"   \\t\\n\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) indicating that the content cannot be parsed as JSON because it contains only whitespace",
                    "description": "A schema consisting solely of whitespace should be treated as invalid JSON and cause a parsing exception."
                }
            ]
        },
        {
            "group": "getSchema(String, InputFormat) - non‑object root node (JSON array)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"[ { \\\"type\\\": \\\"object\\\" } ]\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) because a JSON Schema document must be a JSON object at the root, not an array",
                    "description": "Even if the JSON is syntactically correct, the factory should reject schemas whose top‑level node is not an object."
                }
            ]
        },
        {
            "group": "getSchema(String, InputFormat) - $ref to missing internal definition",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{ \\\"$ref\\\": \\\"#/definitions/missing\\\" }\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the referenced definition cannot be resolved",
                    "description": "When a $ref points to a definition that does not exist within the same document, the factory must raise an exception rather than silently ignore the error."
                }
            ]
        },
        {
            "group": "getSchema(String, InputFormat) - $ref to external unreachable URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{ \\\"$ref\\\": \\\"http://nonexistent.example.com/schema.json\\\" }\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) caused by failure to load the external schema resource",
                    "description": "A $ref that points to an external URI which cannot be fetched should result in a loading exception."
                }
            ]
        },
        {
            "group": "getSchema(String, InputFormat) - circular $ref detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "\"{ \\\"definitions\\\": { \\\"a\\\": { \\\"$ref\\\": \\\"#/definitions/b\\\" }, \\\"b\\\": { \\\"$ref\\\": \\\"#/definitions/a\\\" } }, \\\"$ref\\\": \\\"#/definitions/a\\\" }\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalStateException) indicating a circular reference was detected while resolving $ref",
                    "description": "The factory should detect circular $ref chains and abort with an appropriate exception instead of recursing indefinitely."
                }
            ]
        },
        {
            "group": "getSchema(String, InputFormat) - both parameters null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schema",
                            "value": "null"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) because the schema argument is null, regardless of the input format",
                    "description": "When both arguments are null the method must fail fast on the null schema argument."
                }
            ]
        }
    ]
}