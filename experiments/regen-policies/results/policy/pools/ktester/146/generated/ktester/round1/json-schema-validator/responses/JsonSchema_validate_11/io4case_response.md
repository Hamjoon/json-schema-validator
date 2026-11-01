{
    "group": "JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer) - additional cases",
    "cases": [
        {
            "input": [
                { "parameter": "input", "value": "name: John\nage: 30" },
                { "parameter": "inputFormat", "value": "InputFormat.YAML" },
                { "parameter": "executionCustomizer", "value": "null" }
            ],
            "expected": "Set<ValidationMessage> is empty (no validation errors)",
            "description": "YAML input format – a valid YAML document that maps to the same JSON structure used in the happy‑path test. The method should deserialize the YAML correctly and return an empty error set."
        },
        {
            "input": [
                { "parameter": "input", "value": "%7B%22name%22%3A%22John%22%2C%22age%22%3A%7D" },
                { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED" },
                { "parameter": "executionCustomizer", "value": "null" }
            ],
            "expected": "Set<ValidationMessage> contains a parsing error message (e.g., \"Invalid JSON: Unexpected token\")",
            "description": "URL‑encoded string that decodes to malformed JSON (missing value for \"age\"). After URL decoding, `deserialize` should fail and the method must return a parsing‑error ValidationMessage."
        },
        {
            "input": [
                { "parameter": "input", "value": "[{\"name\":\"John\",\"age\":30}]" },
                { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                { "parameter": "executionCustomizer", "value": "null" }
            ],
            "expected": "Set<ValidationMessage> contains a type‑error message (expected object, found array)",
            "description": "JSON array input – the schema (which expects a JSON object with required properties) receives an array. The validator should report a type‑mismatch error."
        },
        {
            "input": [
                { "parameter": "input", "value": "{\"age\":30}" },
                { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                { "parameter": "executionCustomizer", "value": "customizerDisableStrictMode" }
            ],
            "expected": "Set<ValidationMessage> is empty (no validation errors)",
            "description": "Custom ExecutionContextCustomizer disables strict‑mode (or required‑property enforcement). The input lacks the required \"name\" property, but because strict mode is turned off the validator should not report an error."
        },
        {
            "input": [
                { "parameter": "input", "value": "{\"age\":\"thirty\"}" },
                { "parameter": "inputFormat", "value": "InputFormat.JSON" },
                { "parameter": "executionCustomizer", "value": "customizerEnableFailFast" }
            ],
            "expected": "Set<ValidationMessage> contains exactly one message – the first validation error encountered (e.g., required‑property \"name\" missing)",
            "description": "Custom ExecutionContextCustomizer enables fail‑fast mode. The JSON is missing the required \"name\" property and also has a type error for \"age\". With fail‑fast enabled the validator should stop after the first error, so the result set contains only that single message."
        }
    ]
}