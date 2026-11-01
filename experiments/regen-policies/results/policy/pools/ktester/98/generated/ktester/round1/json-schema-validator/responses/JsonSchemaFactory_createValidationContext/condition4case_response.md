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
          "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build()"
        }
      ],
      "expected": "ValidationContext whose config has discriminatorKeywordEnabled == true and nullableKeywordEnabled == true (the original config is overridden by a new one built with builder(config).discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build())",
      "description": "When the meta‑schema contains the \"discriminator\" keyword and the supplied config has discriminator disabled, the factory must create a new SchemaValidatorsConfig that enables both discriminator and nullable keywords."
    },
    {
      "input": [
        {
          "parameter": "schemaNode",
          "value": "JsonNode whose meta‑schema declares a keyword \"discriminator\""
        },
        {
          "parameter": "config",
          "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(false).build()"
        }
      ],
      "expected": "ValidationContext whose config is the same instance as the supplied config (discriminatorKeywordEnabled == true, nullableKeywordEnabled == false)",
      "description": "When the meta‑schema contains \"discriminator\" but the supplied config already has discriminator enabled, the factory must keep the original config unchanged."
    },
    {
      "input": [
        {
          "parameter": "schemaNode",
          "value": "JsonNode whose meta‑schema does NOT contain the \"discriminator\" keyword"
        },
        {
          "parameter": "config",
          "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build()"
        }
      ],
      "expected": "ValidationContext whose config is the same instance as the supplied config (discriminatorKeywordEnabled == false, nullableKeywordEnabled == false)",
      "description": "When the meta‑schema does not contain the \"discriminator\" keyword, the factory must return a ValidationContext with the original config, regardless of its discriminator flag."
    },
    {
      "input": [
        {
          "parameter": "schemaNode",
          "value": "JsonNode whose meta‑schema does NOT contain the \"discriminator\" keyword"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "NullPointerException thrown (because the method dereferences config.isDiscriminatorKeywordEnabled())",
      "description": "If a null config is supplied, the method should throw NPE – this verifies the method’s defensive behaviour."
    }
  ]
}