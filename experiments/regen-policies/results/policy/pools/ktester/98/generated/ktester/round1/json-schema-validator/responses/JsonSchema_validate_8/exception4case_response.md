{
    "group": "Unresolved $ref handling – exception propagation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "rootNode",
                    "value": "{\"valid\":true}"
                },
                {
                    "parameter": "format",
                    "value": "CollectingOutputFormat (returns List<ValidationMessage>)"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "no‑op Consumer"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The JsonSchema instance is constructed from a schema JSON that contains a $ref pointing to a non‑existent definition. When validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>) is invoked, the resolver should detect the missing reference and throw an IllegalArgumentException. The test verifies that the exception is propagated to the caller."
        }
    ]
}