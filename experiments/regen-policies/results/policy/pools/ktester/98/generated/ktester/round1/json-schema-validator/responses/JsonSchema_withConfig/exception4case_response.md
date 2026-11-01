{
    "test_cases": [
        {
            "group": "Null ValidationContext – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "valid SchemaValidatorsConfig instance"
                        }
                    ],
                    "expected": "Throws NullPointerException because this.getValidationContext() returns null and the method attempts to call getMetaSchema() on it.",
                    "description": "Create a JsonSchema instance whose internal ValidationContext field is null (e.g., via reflection or a test double). Call withConfig(validConfig). The method should dereference a null ValidationContext and raise a NullPointerException."
                }
            ]
        },
        {
            "group": "Null MetaSchema in ValidationContext – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "valid SchemaValidatorsConfig instance"
                        }
                    ],
                    "expected": "Throws NullPointerException because ValidationContext.getMetaSchema() returns null and the method attempts to call getKeywords() on it.",
                    "description": "Mock the JsonSchema's ValidationContext so that getMetaSchema() returns null while getConfig() returns a non‑null config. Invoke withConfig(validConfig) and verify that a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "Null Keywords map in MetaSchema – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "valid SchemaValidatorsConfig instance"
                        }
                    ],
                    "expected": "Throws NullPointerException because MetaSchema.getKeywords() returns null and the method attempts to call containsKey(\"discriminator\").",
                    "description": "Provide a JsonSchema whose ValidationContext returns a MetaSchema whose getKeywords() method is stubbed to return null. Call withConfig(validConfig) and expect a NullPointerException."
                }
            ]
        },
        {
            "group": "Null Config in ValidationContext – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "valid SchemaValidatorsConfig instance"
                        }
                    ],
                    "expected": "Throws NullPointerException because ValidationContext.getConfig() returns null and the method attempts to invoke equals(config) on it.",
                    "description": "Mock the ValidationContext of a JsonSchema so that getConfig() returns null while other parts (metaSchema, keywords) are valid. Invoke withConfig(validConfig) and verify that a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "ValidationContext constructor receives null arguments – NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "valid SchemaValidatorsConfig instance"
                        }
                    ],
                    "expected": "Throws NullPointerException inside the ValidationContext constructor because one of its required arguments (e.g., metaSchema, jsonSchemaFactory, schemaReferences, etc.) is null.",
                    "description": "Arrange a JsonSchema where the first if‑block is skipped, the second if‑condition evaluates to true, and the existing ValidationContext has a null metaSchema (or another required field). When the method tries to create a new ValidationContext with the null argument, the constructor throws a NullPointerException."
                }
            ]
        }
    ]
}