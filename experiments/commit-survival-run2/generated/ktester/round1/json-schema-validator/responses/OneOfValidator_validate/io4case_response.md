{
    "groups": [
        {
            "group": "OneOfValidator.validate - empty schema list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(anyNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "ValidationMessage added with numberOfValidSchema = 0, errors set contains only the OneOf message, collectorContext.evaluatedItems and evaluatedProperties cleared",
                    "description": "When the OneOfValidator has no child schemas, the loop never increments numberOfValidSchema. The validator must return a OneOf error with count 0 and clear evaluation caches."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - mixed valid/invalid schemas (exactly one valid)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "ValidationMessage set empty, numberOfValidSchema = 1, child errors from the invalid schema are NOT added to the result, collectorContext.evaluatedItems/properties remain unchanged",
                    "description": "Two schemas are attached: the first returns an empty error set (valid), the second returns a non‑empty error set (invalid). Because exactly one schema validates, the OneOf validator should succeed and discard the errors from the invalid schema."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - multiple valid schemas, failFast false",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "ValidationMessage added with numberOfValidSchema = 2, errors set empty, collectorContext.evaluatedItems and evaluatedProperties cleared",
                    "description": "Three schemas are added, all of which return empty error sets. After the second schema the loop detects numberOfValidSchema > 1 and breaks. With failFast = false the validator returns a OneOf error (count 2) and no child errors."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - multiple valid schemas, failFast true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "JsonSchemaException thrown containing a OneOf ValidationMessage with numberOfValidSchema = 2",
                    "description": "Same setup as the previous case but the OneOfValidator instance has failFast = true. The method must abort with a JsonSchemaException rather than returning an error set."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - all schemas invalid, failFast true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(invalidNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "JsonSchemaException thrown containing a OneOf ValidationMessage with numberOfValidSchema = 0",
                    "description": "All attached schemas return non‑empty error sets. With failFast = true the validator should throw a JsonSchemaException rather than returning a set that includes the OneOf message and child errors."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - walk enabled, validation disabled (state.isValidationEnabled == false)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=true, validationEnabled=false))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "ValidationMessage set empty, numberOfValidSchema = 1, state.matchedNode == true",
                    "description": "When walk is enabled and validation is disabled, the validator calls schema.walk(..., shouldValidateSchema = false). The mocked schema returns an empty error set, so the validator treats the schema as valid and succeeds."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - walk enabled, validation disabled, schema returns errors",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=true, validationEnabled=false))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(invalidNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "ValidationMessage added with numberOfValidSchema = 0, errors contain childErrors from the schema, collectorContext.evaluatedItems and evaluatedProperties cleared",
                    "description": "Even though validation is disabled, the schema.walk call still returns a non‑empty error set. The validator must treat the schema as invalid, resulting in a OneOf error with count 0 and the collected child errors."
                }
            ]
        },
        {
            "group": "OneOfValidator.validate - state.matchedNode false after walk",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=true, validationEnabled=true, matchedNode=false))"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(validNode)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockJsonNode(root)"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/path\""
                        }
                    ],
                    "expected": "ValidationMessage added with numberOfValidSchema = 0, errors contain childErrors (if any) and OneOf message, collectorContext.evaluatedItems and evaluatedProperties cleared",
                    "description": "Walk is enabled, schema.walk returns an empty error set, but the ValidatorState reports matchedNode = false. The validator must ignore the empty error set, keep numberOfValidSchema at 0, and produce a OneOf error."
                }
            ]
        }
    ]
}