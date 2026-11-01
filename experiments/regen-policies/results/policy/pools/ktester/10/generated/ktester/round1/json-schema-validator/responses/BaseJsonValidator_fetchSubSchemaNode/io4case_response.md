{
    "testCases": [
        {
            "group": "fetchSubSchemaNode - schemaNode boolean true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext.Builder().schemaFactory(JsonSchemaFactory.getInstance()).build()"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.booleanNode(true)"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null) representing a true schema",
                    "description": "When `suppressSubSchemaRetrieval` is false and the validator's `schemaNode` is the boolean literal `true`, `fetchSubSchemaNode` should delegate to `obtainSubSchemaNode` and return a non‑null `JsonSchema` that always validates (the \"true\" schema)."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - empty object schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext.Builder().schemaFactory(JsonSchemaFactory.getInstance()).build()"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null) of type ObjectSchema",
                    "description": "An empty object `{}` is a valid schema that defines an object with no property constraints. With `suppressSubSchemaRetrieval` false the method should return a concrete `JsonSchema` implementation (ObjectSchema) rather than `null`."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - null schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext.Builder().schemaFactory(JsonSchemaFactory.getInstance()).build()"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "If the validator's `schemaNode` field is `null` and `suppressSubSchemaRetrieval` is false, the private helper `obtainSubSchemaNode` receives a `null` argument and must throw a `NullPointerException`. The test confirms that the method does not mask the error."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - $ref resolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext.Builder()\n    .schemaFactory(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))\n    .uriFactory(new UriFactory())\n    .build()"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"$ref\", \"#/definitions/name\").set(\"definitions\", JsonNodeFactory.instance.objectNode().set(\"name\", JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")))"
                        }
                    ],
                    "expected": "instance of JsonSchema (non‑null) whose resolved schemaNode has a \"type\" field equal to \"string\"",
                    "description": "When the validator's `schemaNode` contains a `$ref` to an internal definition, `fetchSubSchemaNode` must resolve the reference via the supplied `ValidationContext`. The returned `JsonSchema` should correspond to the referenced schema (`{\"type\":\"string\"}`), confirming correct reference handling."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - validationContext factory exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext.Builder()\n    .schemaFactory(new JsonSchemaFactory() {\n        @Override\n        public JsonSchema getSchema(JsonNode schemaNode, JsonSchemaFactory factory) {\n            throw new RuntimeException(\"Factory failure\");\n        }\n    })\n    .build()"
                        },
                        {
                            "parameter": "suppressSubSchemaRetrieval",
                            "value": "false"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"string\")"
                        }
                    ],
                    "expected": "RuntimeException with message \"Factory failure\"",
                    "description": "If the `ValidationContext` is built with a custom `JsonSchemaFactory` that throws an exception during schema creation, `fetchSubSchemaNode` should propagate that exception. This test verifies that the method does not catch or alter the factory‑thrown exception."
                }
            ]
        }
    ]
}