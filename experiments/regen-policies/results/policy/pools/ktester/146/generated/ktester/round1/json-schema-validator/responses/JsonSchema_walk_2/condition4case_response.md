{
    "group": "walk(JsonNode, boolean) - basic flag handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "node",
                    "value": "{ \"name\": \"John\", \"age\": 30 }"
                },
                {
                    "parameter": "validate",
                    "value": true
                }
            ],
            "expected": "ValidationResult.success == true and no ValidationMessage",
            "description": "When validate flag is true the schema should be applied to a valid JSON object. Expect a successful ValidationResult with no error messages."
        },
        {
            "input": [
                {
                    "parameter": "node",
                    "value": "{ \"name\": \"John\", \"age\": 30 }"
                },
                {
                    "parameter": "validate",
                    "value": false
                }
            ],
            "expected": "ValidationResult.success == true and no ValidationMessage",
            "description": "When validate flag is false the schema should skip validation and directly return a successful ValidationResult regardless of the node content."
        }
    ]
}