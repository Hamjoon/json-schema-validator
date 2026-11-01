{
    "group": "walk(JsonNode, boolean) - required validator scenarios",
    "cases": [
        {
            "input": [
                { "parameter": "node", "value": "ObjectNode representing { \"name\": \"Bob\" }" },
                { "parameter": "validate", "value": true }
            ],
            "expected": "ValidationResult.failure – contains a ValidationMessage about missing required property `age`",
            "description": "The schema includes a required‑field validator for `age`. The instance omits this property, so walk must report a missing‑required‑property error."
        },
        {
            "input": [
                { "parameter": "node", "value": "ObjectNode representing { \"name\": \"Bob\", \"age\": 25 }" },
                { "parameter": "validate", "value": true }
            ],
            "expected": "ValidationResult.success – empty error set (all required properties are present and satisfy their validators)",
            "description": "Schema has a required validator for `name` and `age`. The instance provides both properties with correct types, so validation succeeds."
        },
        {
            "input": [
                { "parameter": "node", "value": "ObjectNode representing { \"name\": \"Bob\", \"age\": \"twenty\" }" },
                { "parameter": "validate", "value": true }
            ],
            "expected": "ValidationResult.failure – contains two ValidationMessages (type mismatch for `age` and required‑field validation still passes because the field exists)",
            "description": "Schema has both a type validator (age must be integer) and a required validator for `age`. The field exists but with wrong type, so walk should report the type error while not flagging a missing‑required error."
        },
        {
            "input": [
                { "parameter": "node", "value": "null" },
                { "parameter": "validate", "value": false }
            ],
            "expected": "ValidationResult.success – empty error set (validation is disabled, even though node is null)",
            "description": "When `validate` is false the method must bypass all checks and return success regardless of the node being null."
        }
    ]
}