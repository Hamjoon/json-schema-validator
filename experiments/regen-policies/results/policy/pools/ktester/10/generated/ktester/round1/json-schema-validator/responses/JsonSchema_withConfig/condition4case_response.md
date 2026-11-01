{
  "groups": [
    {
      "group": "withConfig_sameConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "sameConfigInstance"
            }
          ],
          "expected": "returns the original JsonSchema instance (reference equality)",
          "description": "The supplied SchemaValidatorsConfig is exactly the same instance that the current ValidationContext holds. The method should detect equality, skip the copy logic, and return `this`."
        }
      ]
    },
    {
      "group": "withConfig_differentConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": "differentConfigInstance"
            }
          ],
          "expected": {
            "instanceDifferent": true,
            "validationContextConfigEqualsProvided": true,
            "validatorsLoaded": false,
            "requiredValidator": null,
            "typeValidator": null,
            "validators": null,
            "initializeConfigCalled": true
          },
          "description": "A distinct SchemaValidatorsConfig is supplied. The method must create a copy of the JsonSchema, replace its ValidationContext with a new one that contains the new config, reset mutable fields (validatorsLoaded, requiredValidator, typeValidator, validators) and invoke `initializeConfig()`. The returned object must not be the same reference as the original."
        }
      ]
    },
    {
      "group": "withConfig_nullConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": {
            "instanceDifferent": true,
            "validationContextConfigIsNull": true,
            "validatorsLoaded": false,
            "requiredValidator": null,
            "typeValidator": null,
            "validators": null,
            "initializeConfigCalled": true
          },
          "description": "A `null` config is passed. Since the current config is non‑null, the equality check fails, triggering the copy path. The new JsonSchema must hold a ValidationContext whose config is `null`, and all mutable fields must be reset just like in the non‑null different‑config case."
        }
      ]
    }
  ]
}