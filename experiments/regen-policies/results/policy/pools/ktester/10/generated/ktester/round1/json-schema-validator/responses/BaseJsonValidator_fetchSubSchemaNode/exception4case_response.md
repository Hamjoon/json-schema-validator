{
    "testCases": [
        {
            "group": "fetchSubSchemaNode - unsupported numeric schemaNode",
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
                            "value": "JsonNodeFactory.instance.numberNode(42)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When `suppressSubSchemaRetrieval` is false and the validator's `schemaNode` is a numeric literal (an unsupported schema type), `obtainSubSchemaNode` should reject it and throw an `IllegalArgumentException`."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - unsupported array schemaNode",
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
                            "value": "JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put(\"type\", \"string\"))"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "An array node is not a valid top‑level JSON Schema representation. The method must propagate an `IllegalArgumentException` when such a node is supplied."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - ValidationContext with null schemaFactory",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext.Builder().schemaFactory(null).build()"
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
                    "expected": "NullPointerException",
                    "description": "If the `ValidationContext` is built without a `JsonSchemaFactory` (i.e., `null`), the internal call to the factory inside `obtainSubSchemaNode` will trigger a `NullPointerException`. The method must not swallow this exception."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - $ref to missing definition",
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
                            "value": "JsonNodeFactory.instance.objectNode().put(\"$ref\", \"#/definitions/unknown\")"
                        }
                    ],
                    "expected": "JsonSchemaException",
                    "description": "When the validator's `schemaNode` contains a `$ref` that points to a non‑existent definition, the resolution process should fail and throw a `JsonSchemaException` (or a subclass such as `IllegalArgumentException`). The test confirms that the method propagates the resolution error."
                }
            ]
        },
        {
            "group": "fetchSubSchemaNode - $ref external unresolved URI",
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
                            "value": "JsonNodeFactory.instance.objectNode().put(\"$ref\", \"http://nonexistent.example.com/schema.json\")"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "An external `$ref` that cannot be fetched (e.g., unknown host) should cause the underlying factory to throw a `RuntimeException` (or `JsonSchemaException`). `fetchSubSchemaNode` must let this exception bubble up."
                }
            ]
        }
    ]
}