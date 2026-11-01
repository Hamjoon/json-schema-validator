{
    "tests": [
        {
            "group": "withConfig_equalDifferentInstance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "equalButDistinctConfigInstance"
                        }
                    ],
                    "expected": "returns the original JsonSchema instance (reference equality)",
                    "description": "The supplied SchemaValidatorsConfig is equal (according to .equals) to the one held by the current ValidationContext but is a different object instance. The method should treat them as the same configuration, skip the copy logic, and return `this`."
                }
            ]
        },
        {
            "group": "withConfig_currentConfigNull_sameNull",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "throws NullPointerException",
                    "description": "The JsonSchema's ValidationContext currently holds a `null` config. Invoking `withConfig(null)` triggers a call to `null.equals(null)`, which results in a NullPointerException. The test verifies that the method does not swallow the exception."
                }
            ]
        },
        {
            "group": "withConfig_currentConfigNull_nonNullProvided",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "config",
                            "value": "nonNullConfigInstance"
                        }
                    ],
                    "expected": "throws NullPointerException",
                    "description": "The JsonSchema's ValidationContext has a `null` config while a non‑null SchemaValidatorsConfig is supplied. The equality check attempts to invoke `null.equals(nonNullConfigInstance)` and therefore throws a NullPointerException. The test ensures this failure path is exercised."
                }
            ]
        },
        {
            "group": "withConfig_copyPreservesContextFields",
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
                        "validationContextMetaSchemaSameAsOriginal": true,
                        "validationContextJsonSchemaFactorySameAsOriginal": true,
                        "validationContextSchemaReferencesSameAsOriginal": true,
                        "validationContextSchemaResourcesSameAsOriginal": true,
                        "validationContextDynamicAnchorsSameAsOriginal": true,
                        "validatorsLoaded": false,
                        "requiredValidator": null,
                        "typeValidator": null,
                        "validators": null,
                        "initializeConfigCalled": true
                    },
                    "description": "When a distinct config is supplied, a new JsonSchema copy is created. Apart from the config, all other parts of the ValidationContext (meta‑schema, factory, references, resources, dynamic anchors) must be preserved exactly (same object references). Mutable validator‑related fields are reset and `initializeConfig()` is invoked."
                }
            ]
        }
    ]
}