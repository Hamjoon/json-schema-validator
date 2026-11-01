{
  "testCases": [
    {
      "group": "Discriminator present, config disabled, nullable already true",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode whose meta‑schema stub contains the \"discriminator\" keyword"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(true).typeLoose(false).build()"
            }
          ],
          "expected": "ValidationContext returned with a new SchemaValidatorsConfig instance where discriminatorKeywordEnabled == true, nullableKeywordEnabled == true, typeLoose == false (preserved), and all other flags unchanged",
          "description": "When the meta‑schema defines \"discriminator\" and the supplied config has discriminator disabled but nullable already enabled, the factory must create a new config that enables discriminator (leaving nullable as true) while preserving any other custom settings."
        }
      ]
    },
    {
      "group": "Discriminator present, config disabled, other flags preserved",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode whose meta‑schema stub contains the \"discriminator\" keyword"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).typeLoose(true).formatKeywordEnabled(false).build()"
            }
          ],
          "expected": "ValidationContext returned with a new SchemaValidatorsConfig instance where discriminatorKeywordEnabled == true, nullableKeywordEnabled == true, typeLoose == true, formatKeywordEnabled == false (preserved)",
          "description": "Ensures that when a new config is created because discriminator was disabled, all other configuration flags supplied by the caller are retained unchanged."
        }
      ]
    },
    {
      "group": "Discriminator absent, config discriminator enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode whose meta‑schema stub does NOT contain the \"discriminator\" keyword"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(false).build()"
            }
          ],
          "expected": "ValidationContext returned with the exact same SchemaValidatorsConfig instance that was passed in (no modification)",
          "description": "If the meta‑schema does not define the \"discriminator\" keyword, the factory must leave the supplied config untouched even when discriminatorKeywordEnabled is true."
        }
      ]
    },
    {
      "group": "Meta‑schema absent, config null leads to NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode (any content, meta‑schema may or may not contain discriminator)"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "NullPointerException thrown because the method dereferences config without null‑check",
          "description": "Verifies the current implementation’s behaviour when a null SchemaValidatorsConfig is supplied; the method is expected to throw NPE."
        }
      ]
    },
    {
      "group": "Discriminator present, config disabled, verify new config instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode whose meta‑schema stub contains the \"discriminator\" keyword"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build()"
            }
          ],
          "expected": "ValidationContext returned with a SchemaValidatorsConfig instance that is NOT the same object as the original config (a new config is created)",
          "description": "Confirms that when the factory needs to enable discriminator, it creates a brand‑new configuration object rather than mutating the original."
        }
      ]
    }
  ]
}