{
    "group": "createValidationContext",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "JsonNode whose meta‑schema declares a keyword \"discriminator\" (e.g., $schema points to a meta‑schema that contains the entry \"discriminator\" in its keywords map)"
                },
                {
                    "parameter": "config",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException thrown (because the method dereferences config.isDiscriminatorKeywordEnabled())",
            "description": "When the schema’s meta‑schema contains the \"discriminator\" keyword but the supplied config is null, the method should still throw NPE – this confirms that the null‑check is not performed before the discriminator flag is inspected."
        },
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "JsonNode whose meta‑schema declares a keyword \"discriminator\""
                },
                {
                    "parameter": "config",
                    "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(true).build()"
                }
            ],
            "expected": "ValidationContext whose config is a NEW instance (different from the supplied config) with discriminatorKeywordEnabled == true and nullableKeywordEnabled == true",
            "description": "The supplied config disables the discriminator keyword but already enables nullable. Because the meta‑schema contains \"discriminator\", the factory must create a fresh config that enables both flags, regardless of the original nullable setting."
        },
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "JsonNode whose meta‑schema does NOT contain the \"discriminator\" keyword"
                },
                {
                    "parameter": "config",
                    "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(false).build()"
                }
            ],
            "expected": "ValidationContext whose config is the SAME instance as the supplied config (discriminatorKeywordEnabled == true, nullableKeywordEnabled == false)",
            "description": "Even though the supplied config enables the discriminator keyword, the meta‑schema does not declare the keyword, so the factory must leave the original config untouched."
        },
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "JsonNode whose meta‑schema does NOT contain the \"discriminator\" keyword"
                },
                {
                    "parameter": "config",
                    "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(true).build()"
                }
            ],
            "expected": "ValidationContext whose config is the SAME instance as the supplied config (discriminatorKeywordEnabled == false, nullableKeywordEnabled == true)",
            "description": "When the meta‑schema lacks the \"discriminator\" keyword, any combination of flags in the supplied config must be preserved unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemaNode",
                    "value": "null"
                },
                {
                    "parameter": "config",
                    "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build()"
                }
            ],
            "expected": "NullPointerException thrown (because the method dereferences schemaNode when selecting the meta‑schema)",
            "description": "Supplying a null schemaNode should cause an NPE, verifying that the method does not guard against a missing schema node before calling getMetaSchemaOrDefault."
        }
    ]
}