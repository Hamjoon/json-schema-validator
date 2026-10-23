{
    "groups": [
        {
            "group": "validate - null executionContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "any non‑null JsonNode (e.g., mock with empty object)"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"#\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Calling validate with a null ExecutionContext should cause a NullPointerException when the method attempts to call executionContext.getCollectorContext()."
                }
            ]
        },
        {
            "group": "validate - null node",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with a valid CollectorContext containing a default ValidatorState"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"#\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the node argument is null, the call to node.get(entry.getKey()) inside the loop will throw a NullPointerException."
                }
            ]
        },
        {
            "group": "validate - collectorContext missing ValidatorState",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext whose getCollectorContext() returns a CollectorContext without the VALIDATOR_STATE_KEY entry"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode with a field matching a defined schema entry"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"#\""
                        }
                    ],
                    "expected": "ClassCastException or NullPointerException",
                    "description": "The method casts the object retrieved from CollectorContext to ValidatorState. If the key is missing, the cast will receive null, leading to a NullPointerException when state.isComplexValidator() is invoked (or a ClassCastException if a wrong type is stored)."
                }
            ]
        },
        {
            "group": "validate - null parent schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with CollectorContext containing a valid ValidatorState"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode that does NOT contain the property defined in schemas"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "at",
                            "value": "\"#\""
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If getParentSchema() returns null, the subsequent call to hasRequiredValidator() will throw a NullPointerException. This scenario can be forced by creating a PropertiesValidator instance whose parent schema reference is deliberately set to null (e.g., via reflection or a test subclass)."
                }
            ]
        },
        {
            "group": "validate - null \"at\" path parameter",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mock ExecutionContext with CollectorContext containing a default ValidatorState"
                        },
                        {
                            "parameter": "node",
                            "value": "mock JsonNode with a field \"name\" present"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "any JsonNode"
                        },
                        {
                            "parameter": "at",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The atPath utility concatenates the provided path with the property key. Passing a null \"at\" argument will cause a NullPointerException inside atPath (or later when adding to evaluatedProperties)."
                }
            ]
        }
    ]
}