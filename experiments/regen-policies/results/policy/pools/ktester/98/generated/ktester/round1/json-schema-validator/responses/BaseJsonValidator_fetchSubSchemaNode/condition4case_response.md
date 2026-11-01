{
    "group": "BaseJsonValidator.fetchSubSchemaNode",
    "cases": [
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "Mocked ValidationContext (no special behavior required)"
                }
            ],
            "expected": "null",
            "description": "When the validator is constructed with suppressSubSchemaRetrieval = true, fetchSubSchemaNode must short‑circuit and return null without invoking obtainSubSchemaNode."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "Mocked ValidationContext (no special behavior required)"
                }
            ],
            "expected": "non‑null JsonSchema instance",
            "description": "When suppressSubSchemaRetrieval = false, fetchSubSchemaNode should delegate to the private static obtainSubSchemaNode method and return the resulting JsonSchema. The test verifies that the returned object is not null and is an instance of JsonSchema."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "Mocked ValidationContext (no special behavior required)"
                }
            ],
            "expected": "null",
            "description": "Edge case: suppressSubSchemaRetrieval = false but the underlying schemaNode is null. The private obtainSubSchemaNode implementation is expected to return null for a null schemaNode, so fetchSubSchemaNode should also return null. This case ensures the method correctly propagates the result of obtainSubSchemaNode."
        }
    ]
}