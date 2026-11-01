Invalid control character at: line 52 column 36 (char 2494)
{
  "testCases": [
    {
      "group": "Discriminator keyword handling – replacement required",
      "cases": [
        {
          "input": [
            {
              "parameter": "originalSchema",
              "value": "jsonSchemaWithMetaContainingDiscriminatorAndContextConfig(discriminatorEnabled=false)"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build()"
            }
          ],
          "expected": "Returned JsonSchema is a new instance (not the same as originalSchema) and its ValidationContext.config has discriminatorKeywordEnabled=true and nullableKeywordEnabled=true",
          "description": "The meta‑schema contains the \"discriminator\" keyword and the supplied config has discriminator disabled. The method must replace the config with a builder that enables both discriminator and nullable, then detect that the new config differs from the original context config and return a newly created JsonSchema."
        }
      ]
    },
    {
      "group": "Discriminator keyword handling – no replacement needed",
      "cases": [
        {
          "input": [
            {
              "parameter": "originalSchema",
              "value": "jsonSchemaWithMetaContainingDiscriminatorAndContextConfig(discriminatorEnabled=true)"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build()"
            }
          ],
          "expected": "Returned JsonSchema is a new instance (not the same as originalSchema) because the supplied config differs from the context config even though no replacement occurs",
          "description": "The meta‑schema contains \"discriminator\" but the supplied config already has it enabled, so the method does not replace the config. Because the supplied config is different from the one stored in the ValidationContext, a new JsonSchema instance must be created."
        }
      ]
    },
    {
      "group": "Config equality – same instance returned",
      "cases": [
        {
          "input": [
            {
              "parameter": "originalSchema",
              "value": "jsonSchemaWithMetaWithoutDiscriminatorAndContextConfig(equalConfig)"
            },
            {
              "parameter": "config",
              "value": "equalConfig // same instance as originalSchema.getValidationContext().getConfig()"
            }
          ],
          "expected": "Returned JsonSchema is the exact same object as originalSchema (reference equality)",
          "description": "The meta‑schema does not contain \"discriminator\" and the supplied config equals the one stored in the ValidationContext. The method should bypass creation of a new schema and simply return `this`."
        }
      ]
    },
    {
      "group": "Config inequality – new instance returned",
      "cases": [
        {
          "input": [
            {
              "parameter": "originalSchema",
              "value": "jsonSchemaWithMetaWithoutDiscriminatorAndContextConfig(baseConfig)"
            },
            {
              "parameter": "config",
              "value": "differentConfig // a distinct SchemaValidatorsConfig instance"
            }
          ],
          "expected": "Returned JsonSchema is a new instance (not the same as originalSchema) and its ValidationContext.config equals differentConfig",
          "description": "The meta‑schema lacks \"discriminator\" and the supplied config differs from the one stored in the ValidationContext. The method must create and return a new JsonSchema that uses the supplied config."
        }
      ]
    }
  ]
}