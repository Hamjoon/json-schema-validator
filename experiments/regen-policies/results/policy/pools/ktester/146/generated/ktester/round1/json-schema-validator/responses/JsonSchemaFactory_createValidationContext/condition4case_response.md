{
  "testCases": [
    {
      "group": "Discriminator keyword present and disabled in config",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode (content irrelevant, meta‑schema is stubbed)"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build()"
            }
          ],
          "expected": "ValidationContext returned with a SchemaValidatorsConfig where discriminatorKeywordEnabled == true and nullableKeywordEnabled == true (original config is unchanged otherwise)",
          "description": "When the meta‑schema contains the \"discriminator\" keyword and the incoming config has discriminatorKeywordEnabled == false, the factory must create a new config enabling both discriminator and nullable keywords."
        }
      ]
    },
    {
      "group": "Discriminator keyword present but already enabled in config",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode (content irrelevant, meta‑schema is stubbed)"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build()"
            }
          ],
          "expected": "ValidationContext returned with the same SchemaValidatorsConfig instance that was passed in (no new config is created)",
          "description": "When the meta‑schema contains the \"discriminator\" keyword but the supplied config already has discriminatorKeywordEnabled == true, the method must keep the original config unchanged."
        }
      ]
    },
    {
      "group": "Meta‑schema does not contain discriminator keyword",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": "mocked JsonNode (content irrelevant, meta‑schema is stubbed without discriminator)"
            },
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build()"
            }
          ],
          "expected": "ValidationContext returned with the same SchemaValidatorsConfig instance that was passed in (no modification)",
          "description": "If the meta‑schema does not define the \"discriminator\" keyword, the method must not alter the incoming config regardless of its discriminatorKeywordEnabled flag."
        }
      ]
    }
  ]
}