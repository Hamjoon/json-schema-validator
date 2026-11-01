{
    "group": "walk(String, InputFormat, boolean, ExecutionContextCustomizer) - Additional Edge & Interaction Cases",
    "cases": [
        {
            "input": [
                { "parameter": "input", "value": "" },
                { "parameter": "inputFormat", "value": "JSON" },
                { "parameter": "validate", "value": true },
                { "parameter": "executionCustomizer", "value": null }
            ],
            "expected": "ValidationResult with isValid() == false and a parsing error message indicating empty input",
            "description": "Empty string should be treated as invalid JSON; the method must return a ValidationResult reflecting the deserialization failure."
        },
        {
            "input": [
                { "parameter": "input", "value": "   " },
                { "parameter": "inputFormat", "value": "JSON" },
                { "parameter": "validate", "value": true },
                { "parameter": "executionCustomizer", "value": null }
            ],
            "expected": "ValidationResult with isValid() == false and a parsing error message indicating whitespace‑only input",
            "description": "Whitespace‑only input is not valid JSON; the method should capture the parsing exception and report it in the ValidationResult."
        },
        {
            "input": [
                { "parameter": "input", "value": "{\"type\":\"object\",\"unknownKeyword\":123}" },
                { "parameter": "inputFormat", "value": "JSON" },
                { "parameter": "validate", "value": true },
                { "parameter": "executionCustomizer", "value": null }
            ],
            "expected": "ValidationResult with isValid() == false and at least one ValidationMessage about an unrecognized keyword",
            "description": "A schema containing an unknown keyword should trigger schema validation errors; the method must return those messages."
        },
        {
            "input": [
                { "parameter": "input", "value": "{\"type\":\"object\"}" },
                { "parameter": "inputFormat", "value": "JSON" },
                { "parameter": "validate", "value": false },
                { "parameter": "executionCustomizer", "value": "customizerAddsFlag" }
            ],
            "expected": "ValidationResult with isValid() == true and the ExecutionContext contains attribute customFlag=true",
            "description": "Even when validation is disabled, the ExecutionContextCustomizer must still be invoked; this verifies that customizer side‑effects are applied irrespective of the validate flag."
        },
        {
            "input": [
                { "parameter": "input", "value": "{\"$id\":\"#root\",\"$defs\":{\"node\":{\"type\":\"object\",\"properties\":{\"next\":{\"$ref\":\"#/$defs/node\"}}}}}" },
                { "parameter": "inputFormat", "value": "JSON" },
                { "parameter": "validate", "value": true },
                { "parameter": "executionCustomizer", "value": null }
            ],
            "expected": "ValidationResult with isValid() == true and no ValidationMessage entries (recursive $ref resolved without infinite recursion)",
            "description": "Schema contains a recursive $ref (self‑reference). The walk method should handle recursive anchors correctly and finish without stack overflow or validation errors."
        },
        {
            "input": [
                { "parameter": "input", "value": "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}" },
                { "parameter": "inputFormat", "value": "JSON" },
                { "parameter": "validate", "value": true },
                { "parameter": "executionCustomizer", "value": "customizerThrowsIllegalStateException" }
            ],
            "expected": "IllegalStateException propagated from the ExecutionContextCustomizer",
            "description": "When the customizer throws a different runtime exception (IllegalStateException), the walk method must let it propagate unchanged."
        }
    ]
}