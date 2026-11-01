{
    "test_cases": [
        {
            "group": "Failure – SchemaLoader returns null SchemaReference",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a valid resource, but the mocked SchemaLoader returns null for getSchema()"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException thrown when the method attempts to invoke getInputStream() on a null SchemaReference.",
                    "description": "The SchemaLoader is stubbed to return null. The try‑with‑resources block then calls .getInputStream() on the null reference, causing an NPE before any IOException handling."
                }
            ]
        },
        {
            "group": "Failure – schemaUri argument is null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException thrown when the method calls schemaUri.getAbsoluteIri().",
                    "description": "Passing a null SchemaLocation should cause an NPE at the first usage of the argument (inside the try block)."
                }
            ]
        },
        {
            "group": "Failure – schemaUri.getAbsoluteIri() returns null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation whose getAbsoluteIri() method is mocked to return null"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException thrown when the method attempts to use the null IRI (e.g., in logger.error or IOException message).",
                    "description": "Even though the SchemaLocation object itself is non‑null, its absolute IRI is null, leading to an NPE during logging or exception construction."
                }
            ]
        },
        {
            "group": "Failure – Config with null PathType",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a valid JSON schema file"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig instance whose getPathType() returns null"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) thrown when constructing new JsonNodePath(null).",
                    "description": "The method creates a JsonNodePath using config.getPathType(). If the path type is null, the constructor should reject it, causing an exception before any schema processing."
                }
            ]
        },
        {
            "group": "Failure – document.getRefSchema(fragment) not found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation with absolute IRI = \"classpath:/schemas/simple.json#\\/nonexistent\" (fragment points to a definition that does not exist)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) thrown by document.getRefSchema() because the fragment cannot be resolved.",
                    "description": "The method loads the whole document successfully, but when it attempts to resolve the fragment to a sub‑schema, the lookup fails and an unchecked exception propagates."
                }
            ]
        },
        {
            "group": "Failure – getMetaSchemaOrDefault receives null schemaNode",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to an empty file (readTree returns null)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException thrown inside getMetaSchemaOrDefault when it tries to inspect a null JsonNode.",
                    "description": "When the input stream is valid but contains no JSON/YAML content, the mapper returns null. Subsequent call to getMetaSchemaOrDefault should fail with NPE."
                }
            ]
        },
        {
            "group": "Failure – SchemaLoader throws unchecked RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "SchemaLocation pointing to a resource whose SchemaLoader.getSchema() throws a RuntimeException (e.g., IllegalStateException)"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalStateException (or the original RuntimeException) propagates out of getMappedSchema without being wrapped.",
                    "description": "Only IOException is caught and wrapped. Any other unchecked exception from SchemaLoader should bubble up unchanged."
                }
            ]
        }
    ]
}