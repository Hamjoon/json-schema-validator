{
    "test_cases": [
        {
            "group": "Null executionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_any"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_any"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method dereferences executionContext immediately (executionContext.getCollectorContext()). Passing a null executionContext should cause a NullPointerException."
                }
            ]
        },
        {
            "group": "Null node parameter",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_noSchemas_noDiscriminator"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_any"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When node is null, the call to typeValidator.equalsToSchemaType(node) (or schema.validate) will throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Null instanceLocation with OpenAPI3 discriminator enabled",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_openapi3_noDiscriminator_nullInstanceLocation"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_object"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_object"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When OpenAPI3 style discriminators are enabled, the method calls executionContext.enterDiscriminatorContext(this.discriminatorContext, instanceLocation). Passing a null instanceLocation should cause a NullPointerException inside the execution context implementation."
                }
            ]
        },
        {
            "group": "OpenAPI3 discriminator enabled but null discriminatorContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_openapi3_nullDiscriminatorContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_object"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_object"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the validator is configured for OpenAPI3 style discriminators but its discriminatorContext field is null, the call executionContext.enterDiscriminatorContext(this.discriminatorContext, …) will throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Schemas list contains null element",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_nullSchemaInList"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_object"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_object"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "AnyOfValidator holds a List<JsonSchema> that may contain a null entry. When the loop reaches that entry, calling schema.getTypeValidator() will throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Schema.validate returns null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_schemaValidateReturnsNull"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_object"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_object"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If a subschema's validate method returns null instead of a Set, the subsequent call errors.isEmpty() will throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Schema.walk returns null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_schemaWalkReturnsNull"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_array"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_array"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When walk is enabled and the subschema's walk method returns null, the later errors.isEmpty() call triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "Null CollectorContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_nullCollectorContext"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_any"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_any"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If executionContext.getCollectorContext() returns null, the first call to collectorContext.enterDynamicScope() will cause a NullPointerException."
                }
            ]
        },
        {
            "group": "Null ValidatorState",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext_nullValidatorState"
                        },
                        {
                            "parameter": "node",
                            "value": "mockNode_any"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "mockRootNode_any"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "mockInstanceLocation_root"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When executionContext.getValidatorState() returns null, any subsequent call to state.hasMatchedNode() or state.setMatchedNode(...) will throw a NullPointerException."
                }
            ]
        }
    ]
}