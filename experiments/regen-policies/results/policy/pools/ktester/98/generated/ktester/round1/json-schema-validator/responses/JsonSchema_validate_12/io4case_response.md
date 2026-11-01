{
    "testCases": [
        {
            "group": "YAML format handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name: John\nage: 30"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
                        }
                    ],
                    "expected": "empty list []",
                    "description": "A well‑formed YAML document that conforms to the schema should be deserialized correctly and result in an empty ValidationMessage list."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name: John\nage: thirty"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<Integer>() { public Integer apply(Set<ValidationMessage> messages) { return messages.size(); } }"
                        }
                    ],
                    "expected": "greater than 0",
                    "description": "YAML input where a field type does not match the schema (age should be numeric) should produce one or more validation messages; the custom OutputFormat returns the count."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "invalid: [unbalanced"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.YAML"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
                        }
                    ],
                    "expected": "com.fasterxml.jackson.core.JsonParseException",
                    "description": "Malformed YAML should cause a JsonParseException during deserialization, which propagates out of validate."
                }
            ]
        },
        {
            "group": "URL_ENCODED valid decoding",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name=John%20Doe&age=30"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> messages) { return messages.isEmpty() ? \"OK\" : \"FAIL\"; } }"
                        }
                    ],
                    "expected": "\"OK\"",
                    "description": "A correctly URL‑encoded string that matches the schema should be decoded, validated, and the OutputFormat should return \"OK\"."
                },
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "name=John%ZZ&age=30"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.URL_ENCODED"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
                        }
                    ],
                    "expected": "java.io.UnsupportedEncodingException",
                    "description": "A URL‑encoded string containing illegal escape sequences should trigger UnsupportedEncodingException during deserialization."
                }
            ]
        },
        {
            "group": "Empty input string handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "\"\""
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<List<ValidationMessage>>() { public List<ValidationMessage> apply(Set<ValidationMessage> messages) { return new ArrayList<>(messages); } }"
                        }
                    ],
                    "expected": "com.fasterxml.jackson.core.JsonParseException",
                    "description": "An empty JSON string is not valid JSON and should cause a JsonParseException."
                }
            ]
        },
        {
            "group": "OutputFormat exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"John\"}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<String>() { public String apply(Set<ValidationMessage> messages) { throw new IllegalStateException(\"formatter failure\"); } }"
                        }
                    ],
                    "expected": "java.lang.IllegalStateException",
                    "description": "If the provided OutputFormat throws a runtime exception, validate should propagate that exception."
                }
            ]
        },
        {
            "group": "Array root validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "[{\"id\":1},{\"id\":2}]"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<Integer>() { public Integer apply(Set<ValidationMessage> messages) { return messages.size(); } }"
                        }
                    ],
                    "expected": "greater than 0",
                    "description": "When the schema expects an object but the JSON root is an array, validation should fail and the custom OutputFormat should return a positive count."
                }
            ]
        },
        {
            "group": "OutputFormat returning null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "input",
                            "value": "{\"name\":\"John\",\"age\":30}"
                        },
                        {
                            "parameter": "inputFormat",
                            "value": "InputFormat.JSON"
                        },
                        {
                            "parameter": "format",
                            "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> messages) { return null; } }"
                        }
                    ],
                    "expected": "null",
                    "description": "A valid document with an OutputFormat that deliberately returns null should result in a null return value from validate."
                }
            ]
        }
    ]
}