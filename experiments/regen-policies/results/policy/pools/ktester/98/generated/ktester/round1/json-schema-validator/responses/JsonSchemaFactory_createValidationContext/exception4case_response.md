{
    "group": "createValidationContext_exceptions",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "JsonNode whose meta‑schema does NOT contain the \"discriminator\" keyword (e.g., $schema points to Draft‑7 meta‑schema)"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException thrown (because ValidationContext constructor receives a null config when the discriminator keyword is absent, leading to a dereference inside ValidationContext)",
            "description": "When the supplied config is null and the resolved meta‑schema does not declare the \"discriminator\" keyword, the short‑circuit in the if‑statement prevents a config.isDiscriminatorKeywordEnabled() call, but the method still passes a null config to ValidationContext, which is expected to raise NPE."
        },
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "JsonNode (any non‑null node) – the test overrides getMetaSchemaOrDefault to return null"
                },
                {
                    "parameter": "config",
                    "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build()"
                }
            ],
            "expected": "NullPointerException thrown (because jsonMetaSchema is null and the method attempts to call jsonMetaSchema.getKeywords())",
            "description": "If getMetaSchemaOrDefault unexpectedly returns null, the subsequent call to jsonMetaSchema.getKeywords() triggers an NPE."
        },
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "JsonNode (any non‑null node) – the test overrides getMetaSchemaOrDefault to return a JsonMetaSchema whose getKeywords() returns null"
                },
                {
                    "parameter": "config",
                    "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build()"
                }
            ],
            "expected": "NullPointerException thrown (because jsonMetaSchema.getKeywords() returns null and the method invokes containsKey on it)",
            "description": "When the resolved JsonMetaSchema has a null keywords map, the call to jsonMetaSchema.getKeywords().containsKey(\"discriminator\") results in an NPE."
        }
    ]
}