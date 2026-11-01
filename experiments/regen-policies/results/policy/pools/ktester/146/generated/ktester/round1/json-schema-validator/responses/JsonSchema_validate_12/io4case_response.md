{
    "tests": [
        {
            "group": "NullOutputFormat",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"Bob\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": null
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "A null OutputFormat cannot be used; the method should throw a NullPointerException before attempting validation."
                }
            ]
        },
        {
            "group": "MissingRequiredProperty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"age\":25}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "[\"required property 'name' is missing\"]",
                    "description": "The schema defines a required property 'name'. Providing JSON without this property should result in a ValidationMessage list containing the appropriate required‑property error."
                }
            ]
        },
        {
            "group": "ValidYamlInput",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name: Charlie\nage: 40"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "[]",
                    "description": "When the InputFormat is YAML and the YAML content conforms to the schema, validate should return an empty list of ValidationMessage."
                }
            ]
        },
        {
            "group": "EmptyStringInputThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": ""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "com.fasterxml.jackson.core.JsonParseException",
                    "description": "An empty string is not valid JSON; deserialization should fail with JsonParseException which propagates out of validate."
                }
            ]
        },
        {
            "group": "WhitespaceOnlyInputThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "   \n\t  "
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "com.fasterxml.jackson.core.JsonParseException",
                    "description": "A string containing only whitespace cannot be parsed as JSON; JsonParseException should be thrown."
                }
            ]
        },
        {
            "group": "UnicodeJsonInput",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"José\",\"city\":\"München\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "[]",
                    "description": "JSON containing Unicode characters should be deserialized correctly and, if it satisfies the schema, produce an empty ValidationMessage list."
                }
            ]
        },
        {
            "group": "LargeJsonInputValid",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"items\":[{\"id\":1,\"value\":\"a\"},{\"id\":2,\"value\":\"b\"},{\"id\":3,\"value\":\"c\"},{\"id\":4,\"value\":\"d\"},{\"id\":5,\"value\":\"e\"}],\"count\":5}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "SimpleListOutputFormat"
                        }
                    ],
                    "expected": "[]",
                    "description": "A relatively large but valid JSON payload should still be processed correctly and return an empty ValidationMessage list when it conforms to the schema."
                }
            ]
        }
    ]
}