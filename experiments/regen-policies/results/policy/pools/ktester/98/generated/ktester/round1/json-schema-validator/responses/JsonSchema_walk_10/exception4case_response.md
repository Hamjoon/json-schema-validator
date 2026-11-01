{
    "group": "Null ValidationContext",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "null"
                },
                {
                    "parameter": "node",
                    "value": "mocked JsonNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "mocked JsonNode"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "mocked JsonNodePath"
                },
                {
                    "parameter": "shouldValidateSchema",
                    "value": "true"
                }
            ],
            "expected": "NullPointerException propagated because JsonSchema.validationContext is null when walk() tries to access validationContext.getConfig()",
            "description": "The JsonSchema instance is created with a null ValidationContext. When walk() is invoked it attempts to call validationContext.getConfig(), causing a NullPointerException."
        }
    ]
}