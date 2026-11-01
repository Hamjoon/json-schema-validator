{
    "groups": [
        {
            "group": "Invalid id format (SchemaLocation.of throws IllegalArgumentException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"not-a-valid-uri\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or URISyntaxException wrapped) is thrown because the supplied id cannot be parsed into a valid URI by `SchemaLocation.of`.",
                    "description": "Pass a string that is not a valid URI to `loadMetaSchema`. The method should fail fast when `SchemaLocation.of(id)` validates the argument."
                }
            ]
        },
        {
            "group": "Schema not found (getSchema returns null → NullPointerException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/missing-schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException is thrown when the method attempts to call `schema.getValidationContext()` on a null `JsonSchema` returned by `getSchema`.",
                    "description": "Provide an id that does not correspond to any registered schema resource. `getSchema` returns null, causing the subsequent dereference to raise NPE."
                }
            ]
        },
        {
            "group": "Schema node is null (NPE when accessing $vocabulary)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/null-schema-node\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException is thrown when the method executes `schema.getSchemaNode().get(\"$vocabulary\")` because `getSchemaNode()` returns null.",
                    "description": "Mock or configure a schema such that its internal `JsonNode` representation is null. The method should propagate the NPE when trying to read `$vocabulary`."
                }
            ]
        },
        {
            "group": "$vocabulary node is not an object (e.g., array) → IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/vocab-not-object\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or UnsupportedOperationException) is thrown when the code attempts to iterate over `vocabulary.properties()` on a non‑object node.",
                    "description": "Use a meta‑schema whose `$vocabulary` field is an array or primitive value. The call to `properties()` expects an object node and should raise an exception."
                }
            ]
        },
        {
            "group": "MetaSchema is null (builder receives null meta schema → NullPointerException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "id",
                            "value": "\"http://example.com/null-meta-schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException is thrown when `JsonMetaSchema.builder(id, null)` is invoked because the underlying `schema.getValidationContext().getMetaSchema()` returned null.",
                    "description": "Configure a test schema whose `ValidationContext` does not contain a `MetaSchema` (returns null). The builder call should fail with NPE."
                }
            ]
        }
    ]
}