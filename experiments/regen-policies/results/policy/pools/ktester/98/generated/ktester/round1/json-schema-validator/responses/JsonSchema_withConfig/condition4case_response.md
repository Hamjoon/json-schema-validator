{
  "testCases": [
    {
      "group": "First if condition true – discriminator keyword present and disabled in config",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with discriminatorKeywordEnabled=false, nullableKeywordEnabled=false"
            }
          ],
          "expected": "Returned JsonSchema instance is NOT the same as the original (new instance). The config used inside the new instance has discriminatorKeywordEnabled=true and nullableKeywordEnabled=true (builder auto‑enabled).",
          "description": "Create a JsonSchema whose ValidationContext.metaSchema.keywords contains the key \"discriminator\". Pass a config where isDiscriminatorKeywordEnabled() returns false. The method should enter the first if‑block, build a new config with both discriminatorKeywordEnabled and nullableKeywordEnabled set to true, and because this new config differs from the original ValidationContext.config, the second if‑block creates and returns a new JsonSchema instance."
        }
      ]
    },
    {
      "group": "First if condition false – discriminator keyword absent in meta‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with discriminatorKeywordEnabled=false, nullableKeywordEnabled=false"
            }
          ],
          "expected": "Returned JsonSchema instance is NOT the same as the original (new instance) because the supplied config differs from the current ValidationContext.config.",
          "description": "Mock ValidationContext.metaSchema.keywords to NOT contain \"discriminator\". Pass a config with discriminator disabled. The first if‑condition evaluates to false, so the original config is used. Since the supplied config is different from the context's config, the second if‑condition is true and a new JsonSchema instance is returned."
        },
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig identical to the one held by ValidationContext.config"
            }
          ],
          "expected": "Returned JsonSchema instance is the same object (this).",
          "description": "Meta‑schema does not contain \"discriminator\" and the passed config equals the config already stored in ValidationContext. Both if‑conditions evaluate to false, so the method returns `this`."
        }
      ]
    },
    {
      "group": "First if condition false – discriminator keyword present but already enabled in config",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with discriminatorKeywordEnabled=true, nullableKeywordEnabled=true"
            }
          ],
          "expected": "Returned JsonSchema instance is NOT the same as the original (new instance) because the supplied config differs from the current ValidationContext.config.",
          "description": "Mock ValidationContext.metaSchema.keywords to contain \"discriminator\". Pass a config where isDiscriminatorKeywordEnabled() returns true, so the first if‑block is skipped. Since the passed config is different from the one stored in ValidationContext, the second if‑condition creates a new JsonSchema instance."
        },
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig identical to the one held by ValidationContext.config (discriminator enabled)"
            }
          ],
          "expected": "Returned JsonSchema instance is the same object (this).",
          "description": "Meta‑schema contains \"discriminator\" but the supplied config already has discriminator enabled and is equal to the context's config. Both conditions are false; the method returns `this`."
        }
      ]
    },
    {
      "group": "Second if condition true – config changed after first if (or originally different)",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig with discriminatorKeywordEnabled=false, nullableKeywordEnabled=false"
            }
          ],
          "expected": "Returned JsonSchema instance is a newly constructed object (different reference). The internal ValidationContext uses the (possibly rebuilt) config.",
          "description": "Arrange a JsonSchema whose current ValidationContext.config is NOT equal to the supplied config (e.g., the context config has discriminatorKeywordEnabled=true). The method should skip the first if‑block (or rebuild config) and then satisfy the second condition, constructing a new JsonSchema with the supplied config."
        }
      ]
    },
    {
      "group": "Second if condition false – supplied config equals current context config",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "SchemaValidatorsConfig exactly equal to ValidationContext.config"
            }
          ],
          "expected": "Returned JsonSchema instance is the same reference as the original (this).",
          "description": "Mock ValidationContext.getConfig() to return a specific SchemaValidatorsConfig instance. Pass that exact instance (or an equal one) to withConfig. Neither the first nor the second if‑condition becomes true, so the method returns the original JsonSchema."
        }
      ]
    }
  ]
}