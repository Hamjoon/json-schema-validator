{
    "result": [
        {
            "group": "Union type handling – null UnionTypeValidator",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (collectorContext with empty sets)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNodeFactory.instance.textNode(\"any\")"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/some/path\""
                        }
                    ],
                    "expected": "NullPointerException when schemaType == JsonType.UNION and unionTypeValidator is null",
                    "description": "The validator’s schemaType is set to UNION but the UnionTypeValidator instance has not been injected (null). Calling validate() should throw a NullPointerException when it attempts to invoke unionTypeValidator.validate(...)."
                }
            ]
        },
        {
            "group": "Schema path null – endsWith call",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (collectorContext with empty sets)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNodeFactory.instance.textNode(\"match\")"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/prop\""
                        }
                    ],
                    "expected": "NullPointerException when this.schemaPath is null and the code executes this.schemaPath.endsWith(\"/type\")",
                    "description": "When the validator is constructed without a schemaPath (null), the conditional check schemaPath.endsWith(\"/type\") triggers an NPE. The test verifies that the exception is thrown."
                }
            ]
        },
        {
            "group": "Root node null – isArray / isObject call",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (collectorContext with empty sets)"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNodeFactory.instance.textNode(\"match\")"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "null"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/0\""
                        }
                    ],
                    "expected": "NullPointerException when rootNode.isArray() or rootNode.isObject() is invoked",
                    "description": "If the rootNode argument is null, the validator reaches the hack block (when schemaPath ends with \"/type\") and attempts to call isArray()/isObject() on a null reference, causing an NPE."
                }
            ]
        },
        {
            "group": "ExecutionContext null – getCollectorContext call",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNodeFactory.instance.textNode(\"match\")"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/prop\""
                        }
                    ],
                    "expected": "NullPointerException when executionContext.getCollectorContext() is invoked",
                    "description": "Passing a null ExecutionContext causes an NPE at the point where the validator tries to obtain the CollectorContext for evaluated items/properties."
                }
            ]
        },
        {
            "group": "CollectorContext null – evaluated set access",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext where getCollectorContext() returns null"
                        },
                        {
                            "parameter": "node",
                            "value": "JsonNodeFactory.instance.textNode(\"match\")"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.arrayNode()"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/0\""
                        }
                    ],
                    "expected": "NullPointerException when attempting to call getEvaluatedItems() on a null CollectorContext",
                    "description": "Even with a non‑null ExecutionContext, if its CollectorContext is null the validator will throw an NPE while trying to record the evaluated location."
                }
            ]
        },
        {
            "group": "Node null – equalsToSchemaType / TypeFactory call",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "executionContext",
                            "value": "mockExecutionContext (collectorContext with empty sets)"
                        },
                        {
                            "parameter": "node",
                            "value": "null"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "at",
                            "value": "\"/missing\""
                        }
                    ],
                    "expected": "NullPointerException either inside equalsToSchemaType(node) or TypeFactory.getValueNodeType(node, ...) when node is null",
                    "description": "Providing a null JsonNode as the node to validate should cause an NPE because the validator assumes a non‑null node when checking its type."
                }
            ]
        }
    ]
}