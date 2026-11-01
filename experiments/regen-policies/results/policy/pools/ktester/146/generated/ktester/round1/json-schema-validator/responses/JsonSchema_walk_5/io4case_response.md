{
    "newTestCases": [
        {
            "group": "Malformed JSON with validation disabled",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "{\"name\":\"John\", \"age\":}" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": false }
                    ],
                    "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages().size() > 0",
                    "description": "Even when validation is turned off, a syntactically incorrect JSON string cannot be deserialized, so walk should return an invalid ValidationResult with parsing error messages."
                }
            ]
        },
        {
            "group": "Empty string input",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages().size() > 0",
                    "description": "An empty string is not valid JSON; deserialization should fail and walk should report the error in the ValidationResult."
                }
            ]
        },
        {
            "group": "Whitespace‑only input",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "   \t\n  " },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages().size() > 0",
                    "description": "A string containing only whitespace is not a valid JSON document; walk should treat it as a parsing failure."
                }
            ]
        },
        {
            "group": "Null InputFormat",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "{\"key\":\"value\"}" },
                        { "parameter": "inputFormat", "value": null },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "throws IllegalArgumentException",
                    "description": "The method should reject a null InputFormat argument early, throwing an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Valid YAML input (supported format)",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "name: John\nage: 30" },
                        { "parameter": "inputFormat", "value": "InputFormat.YAML" },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
                    "description": "When the library supports YAML, a well‑formed YAML document should be deserialized and validated successfully."
                }
            ]
        },
        {
            "group": "JSON array input",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "[{\"id\":1},{\"id\":2}]" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
                    "description": "A valid JSON array should be accepted and, with validation enabled, produce a successful ValidationResult when the schema does not restrict the root type."
                },
                {
                    "input": [
                        { "parameter": "input", "value": "[{\"id\":1},{\"id\":2}]" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": false }
                    ],
                    "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
                    "description": "Even with validation disabled, a correctly formed JSON array should result in a valid ValidationResult."
                }
            ]
        },
        {
            "group": "JSON primitive number input",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "12345" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
                    "description": "A JSON document consisting of a single number is syntactically valid; with a permissive schema walk should return a successful ValidationResult."
                }
            ]
        },
        {
            "group": "JSON primitive boolean input",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "true" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
                    "description": "A JSON document that is a boolean literal should deserialize correctly and, assuming no type constraints, produce a valid ValidationResult."
                }
            ]
        },
        {
            "group": "JSON null literal input",
            "cases": [
                {
                    "input": [
                        { "parameter": "input", "value": "null" },
                        { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                        { "parameter": "validate", "value": true }
                    ],
                    "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
                    "description": "The JSON literal `null` is a valid JSON document; walk should handle it without errors and return a successful ValidationResult when the schema permits null."
                }
            ]
        }
    ]
}