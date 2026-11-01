{
    "test_cases": [
        {
            "group": "Constructor validation exceptions",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(valid=true, type=ObjectNode)"
                        }
                    ],
                    "expected": "IllegalStateException with message \"schemaNode must not be null\"",
                    "description": "If a BaseJsonValidator (or subclass) is instantiated with a null `schemaNode`, the `validate` method should detect the missing schema and throw an IllegalStateException before any validation logic runs."
                }
            ]
        },
        {
            "group": "atRoot() returning null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(atRootReturnsNull=true)"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(valid=true, type=ObjectNode)"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the overridden `atRoot()` method returns `null`, the internal call that expects a non‑null `JsonNodePath` triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(fetchSubSchemaThrows=true)"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(valid=true, type=ObjectNode)"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"invalid sub‑schema reference\"",
                    "description": "If a validator's `fetchSubSchemaNode` implementation throws an IllegalArgumentException (e.g., because a $ref cannot be resolved), `validate` must let the exception propagate unchanged."
                }
            ]
        },
        {
            "group": "Discriminator object type mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(withDiscriminator=true, discriminatorObjectType=ArrayNode, discriminatorProperty=\"type\", discriminatorValue=\"A\")"
                        }
                    ],
                    "expected": "ClassCastException",
                    "description": "When a node contains a discriminator field that is not an `ObjectNode` (e.g., an `ArrayNode`), the static helper `checkDiscriminatorMatch` casts it and throws a ClassCastException."
                }
            ]
        },
        {
            "group": "Discriminator property non‑textual value",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext()"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(withDiscriminator=true, discriminatorProperty=\"type\", discriminatorValue=12345)"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Discriminator property 'type' must be a textual value\"",
                    "description": "If the discriminator property exists but its value is not a text node (e.g., a numeric node), `checkDiscriminatorMatch` should raise an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Debug logging with null logger",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(logger=null, loggerDebugEnabled=true)"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(valid=true, type=ObjectNode)"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When debug logging is enabled but the provided `Logger` instance is `null`, the static `debug` method attempts to call `logger.isDebugEnabled()` and throws a NullPointerException."
                }
            ]
        },
        {
            "group": "Annotation customizer null handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext(collectAnnotations=true, customizer=null)"
                        },
                        {
                            "parameter": "node",
                            "value": "mockJsonNode(valid=true, type=ObjectNode)"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If `collectAnnotations` returns true but the validator invokes `putAnnotation` with a `null` customizer, the internal `Consumer.accept` call results in a NullPointerException."
                }
            ]
        }
    ]
}