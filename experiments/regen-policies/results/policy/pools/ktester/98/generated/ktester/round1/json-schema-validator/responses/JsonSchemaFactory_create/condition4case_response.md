{
    "testCases": [
        {
            "group": "Null validationContext",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "null"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'validationContext must not be null'",
                    "description": "The factory should reject a null ValidationContext because it is required for schema creation."
                }
            ]
        },
        {
            "group": "Null schemaLocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "null"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.objectNode()"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'schemaLocation must not be null'",
                    "description": "When schemaLocation is null the factory cannot resolve relative URIs, so it must throw."
                }
            ]
        },
        {
            "group": "Null schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'schemaNode must not be null'",
                    "description": "A null JsonNode cannot be interpreted as a schema; the method must reject it."
                }
            ]
        },
        {
            "group": "Simple object schema without $ref",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/simple-schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectNode().put(\"type\", \"object\").set(\"properties\", objectNode().set(\"name\", objectNode().put(\"type\", \"string\")))"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchema instance whose root schema node equals the supplied schemaNode and whose meta‑schema is the default (draft‑v7)",
                    "description": "Standard happy‑path: a plain object schema should be wrapped into a JsonSchema without any special branching."
                }
            ]
        },
        {
            "group": "Schema with $ref to external schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/main-schema.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectNode().put(\"$ref\", \"classpath:/schemas/definitions.json#/definitions/address\")"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchema instance whose underlying schemaNode is the resolved address definition (type object with properties street, city)",
                    "description": "When a $ref is present the factory must follow the reference, load the target schema and return the resolved JsonSchema."
                }
            ]
        },
        {
            "group": "Recursive $ref (self‑reference)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/recursive.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectNode().put(\"type\", \"object\").set(\"properties\", objectNode().set(\"node\", objectNode().put(\"$ref\", \"#\")))"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "JsonSchema instance that contains a self‑referencing validator without causing StackOverflowError",
                    "description": "The factory must detect recursion and handle it gracefully (usually by creating a placeholder and later linking it)."
                }
            ]
        },
        {
            "group": "Parent schema provided (sub‑schema creation)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/parent.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.of(\"/properties/address\")"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "objectNode().put(\"type\", \"object\").set(\"properties\", objectNode().set(\"street\", objectNode().put(\"type\", \"string\")))"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "parentJsonSchema"
                        }
                    ],
                    "expected": "JsonSchema instance whose parent is the supplied parentJsonSchema and whose evaluationPath matches the provided path",
                    "description": "When a parent schema is supplied, the created schema must keep a reference to it, enabling proper resolution of relative $ref values."
                }
            ]
        },
        {
            "group": "Unsupported schema node type (e.g., array node as root)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "validationContext",
                            "value": "new ValidationContext()"
                        },
                        {
                            "parameter": "schemaLocation",
                            "value": "new SchemaLocation(\"classpath:/schemas/array-root.json\")"
                        },
                        {
                            "parameter": "evaluationPath",
                            "value": "JsonNodePath.ROOT"
                        },
                        {
                            "parameter": "schemaNode",
                            "value": "JsonNodeFactory.instance.arrayNode().add(objectNode().put(\"type\", \"string\"))"
                        },
                        {
                            "parameter": "parentSchema",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing 'Root schema must be an object'",
                    "description": "The factory expects the root schema node to be an object; an array node should trigger an error branch."
                }
            ]
        }
    ]
}