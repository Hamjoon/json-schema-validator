{
    "group": "JsonSchemaFactory.create – basic forwarding and branch coverage",
    "cases": [
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"object\\\" }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema instance whose meta‑schema version matches the default version (no exception)",
            "description": "Happy‑path: all arguments are non‑null and the schema node describes a simple object. The call should be forwarded to doCreate and return a valid JsonSchema."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "null"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"string\\\" }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException (or IllegalArgumentException) because ValidationContext is required",
            "description": "Branch where the first argument is null – doCreate validates the context and should abort with an exception."
        },
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
                    "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"integer\\\" }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException (or IllegalArgumentException) because SchemaLocation is required",
            "description": "Branch where schemaLocation is null – the factory cannot resolve relative references and must reject the call."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ \\\"$ref\\\": \\\"#/definitions/foo\\\" }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema that resolves the $ref to the definition (no exception)",
            "description": "Branch that triggers reference resolution inside doCreate – the method must follow the $ref path and produce a schema that delegates to the referenced definition."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"array\\\", \\\"items\\\": { \\\"type\\\": \\\"boolean\\\" } }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema with an ItemsValidator for the nested type (no exception)",
            "description": "Branch that creates a composite schema (array with items). The factory must recurse into the `items` node and build a child schema."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema representing an empty schema (accepts any JSON) (no exception)",
            "description": "Branch where the schema node is empty – the factory should return a schema that imposes no constraints."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"string\\\", \\\"enum\\\": [\\\"a\\\", \\\"b\\\"] }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema that contains an EnumValidator with two allowed values (no exception)",
            "description": "Branch that combines `type` and `enum` keywords – the factory must create both validators and attach them to the schema."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ \\\"type\\\": \\\"object\\\", \\\"properties\\\": { \\\"id\\\": { \\\"type\\\": \\\"integer\\\" } }, \\\"required\\\": [\\\"id\\\"] }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema with PropertyValidators for `id` and a RequiredValidator (no exception)",
            "description": "Branch that creates an object schema with properties and a required list – multiple internal validators must be instantiated."
        },
        {
            "input": [
                {
                    "parameter": "validationContext",
                    "value": "new ValidationContext()"
                },
                {
                    "parameter": "schemaLocation",
                    "value": "new SchemaLocation(\"classpath:/schema.json\")"
                },
                {
                    "parameter": "evaluationPath",
                    "value": "JsonNodePath.ROOT"
                },
                {
                    "parameter": "schemaNode",
                    "value": "objectMapper.readTree(\"{ \\\"if\\\": { \\\"properties\\\": { \\\"type\\\": { \\\"const\\\": \\\"A\\\" } } }, \\\"then\\\": { \\\"required\\\": [\\\"a\\\"] }, \\\"else\\\": { \\\"required\\\": [\\\"b\\\"] } }\")"
                },
                {
                    "parameter": "parentSchema",
                    "value": "null"
                }
            ],
            "expected": "non‑null JsonSchema that contains an IfThenElseValidator (no exception)",
            "description": "Branch that handles conditional subschemas (`if`/`then`/`else`). The factory must parse each branch and link them appropriately."
        }
    ]
}