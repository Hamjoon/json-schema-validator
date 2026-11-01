{
  "testCases": [
    {
      "group": "First if true – rebuilt config equals current ValidationContext.config (no new instance)",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with discriminatorKeywordEnabled=false, nullableKeywordEnabled=false"
            }
          ],
          "expected": "Returned JsonSchema instance is the same object (this). The meta‑schema contains the \"discriminator\" keyword, the method rebuilds the config (enabling discriminator and nullable). Because the original ValidationContext already holds a config with discriminatorKeywordEnabled=true and nullableKeywordEnabled=true, the rebuilt config equals the existing one, so the second if‑condition is false.",
          "description": "Create a JsonSchema whose ValidationContext.metaSchema.keywords includes \"discriminator\" and whose current ValidationContext.config already has discriminatorKeywordEnabled=true and nullableKeywordEnabled=true. Pass a config where both flags are false. The method should enter the first if‑block, rebuild the config to enable the flags, detect that this rebuilt config is equal to the current context config, skip the second if‑block and return the original instance."
        }
      ]
    },
    {
      "group": "Meta‑schema without discriminator – config enables discriminator (new instance)",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with discriminatorKeywordEnabled=true, nullableKeywordEnabled=false"
            }
          ],
          "expected": "Returned JsonSchema instance is a newly constructed object (different reference). Because the meta‑schema does not contain the \"discriminator\" keyword, the first if‑block is skipped. The supplied config differs from the ValidationContext.config, so the second if‑condition creates a new JsonSchema.",
          "description": "Mock ValidationContext.metaSchema.keywords to NOT contain \"discriminator\". Ensure the current ValidationContext.config has discriminatorKeywordEnabled=false. Pass a config with discriminatorKeywordEnabled=true. The method should return a new JsonSchema instance."
        },
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig identical to the one held by ValidationContext.config (discriminator enabled)"
            }
          ],
          "expected": "Returned JsonSchema instance is the same object (this). Both if‑conditions evaluate to false because the meta‑schema lacks \"discriminator\" and the supplied config equals the current context config.",
          "description": "Same setup as above but the ValidationContext.config already has discriminatorKeywordEnabled=true and nullableKeywordEnabled=false. Pass the exact same config instance (or an equal one). The method should return the original JsonSchema."
        }
      ]
    },
    {
      "group": "Config differs only in nullableKeywordEnabled flag (new instance)",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with nullableKeywordEnabled=true (all other flags same as current context config)"
            }
          ],
          "expected": "Returned JsonSchema instance is a new instance (different reference). The first if‑block is not triggered (discriminator keyword state unchanged). The second if‑condition sees the config inequality and creates a new JsonSchema.",
          "description": "Set up a JsonSchema whose ValidationContext.config has nullableKeywordEnabled=false. Pass a config that is identical except nullableKeywordEnabled=true. Verify that a new JsonSchema is returned."
        }
      ]
    },
    {
      "group": "Config differs only in a non‑discriminator flag (e.g., typeLoose) – new instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with typeLoose=true (all other flags same as current context config)"
            }
          ],
          "expected": "Returned JsonSchema instance is a new instance (different reference). The first if‑block is skipped because discriminator keyword handling is unchanged. The second if‑condition detects the inequality of the supplied config and constructs a new JsonSchema.",
          "description": "Assume the current ValidationContext.config has typeLoose=false. Pass a config where only typeLoose is true. Verify that the method returns a newly created JsonSchema."
        }
      ]
    },
    {
      "group": "Null config argument – NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException when invoking withConfig(null). The method dereferences config to call isDiscriminatorKeywordEnabled(), causing the NPE.",
          "description": "Call withConfig(null) on any JsonSchema instance. The test should assert that a NullPointerException is thrown."
        }
      ]
    }
  ]
}