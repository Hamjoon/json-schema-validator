{
    "test_cases": [
        {
            "group": "Null ExecutionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "any valid JsonNode (e.g., JsonNodeFactory.instance.textNode(\"test\"))"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The method dereferences executionContext at the very beginning (enterDiscriminatorContext, isFailFast, setFailFast, etc.). Passing a null executionContext must cause a NullPointerException."
                }
            ]
        },
        {
            "group": "Null ValidationContext Config",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext (failFast=true) with a ValidationContext whose getConfig() returns null"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The code accesses validationContext.getConfig().isDiscriminatorKeywordEnabled(). If getConfig() returns null, a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "Null Schemas List",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext (failFast=true, discriminator disabled)"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "AnyOfValidator.schemas is null. The for‑each loop over this.schemas triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "Schemas List Contains Null Element",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext (failFast=true, discriminator disabled)"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The schemas list contains a null entry. When the loop reaches that entry, calling schema.getTypeValidator() on a null schema throws NullPointerException."
                }
            ]
        },
        {
            "group": "Discriminator Enabled but No DiscriminatorContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with discriminator enabled, but getCurrentDiscriminatorContext() returns null"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When discriminator is enabled the code calls executionContext.getCurrentDiscriminatorContext().isDiscriminatorMatchFound(). If the context is null, a NullPointerException is thrown."
                }
            ]
        },
        {
            "group": "DiscriminatorContext Methods Throw RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with discriminator enabled; getCurrentDiscriminatorContext() returns a mock that throws RuntimeException on isDiscriminatorMatchFound()"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "If the DiscriminatorContext implementation throws an unchecked exception while checking match/ignore flags, the exception propagates out of validate()."
                }
            ]
        },
        {
            "group": "Schema.validate Throws Exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext (failFast=true, discriminator disabled)"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "One of the subschemas is a mock that throws IllegalStateException from its validate() method. The AnyOfValidator does not catch it, so the exception should propagate."
                }
            ]
        },
        {
            "group": "Schema.walk Throws Exception (walk=true)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext (failFast=true, discriminator disabled)"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": true
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "A subschema mock throws RuntimeException from its walk() method. Since the validator calls walk() when walk flag is true, the exception should bubble up."
                }
            ]
        },
        {
            "group": "canShortCircuit Methods Throw Exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext (failFast=true, discriminator disabled)"
                        },
                        {
                            "parameter": "node",
                            "value": "any JsonNode that would otherwise validate successfully"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "same as node"
                        },
                        {
                            "parameter": "instanceLocation",
                            "value": "JsonNodePath.root()"
                        },
                        {
                            "parameter": "walk",
                            "value": false
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "AnyOfValidator is subclassed in the test so that canShortCircuit() throws a RuntimeException. When the first subschema validates without errors, the validator attempts to call canShortCircuit() and the exception propagates."
                }
            ]
        }
    ]
}