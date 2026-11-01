{
    "tests": [
        {
            "group": "getSchema(SchemaLocation) - caching behavior",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/cached-schema.json"
                        }
                    ],
                    "expected": "The first and second calls to getSchema with the same SchemaLocation return the identical JsonSchema instance (reference equality), demonstrating that the factory caches loaded schemas.",
                    "description": "Verifies that the factory does not reload a schema that has already been parsed and cached, which is important for performance and for preserving identity semantics across the application."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - HTTP URI handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "http://localhost:8080/remote-schema.json"
                        }
                    ],
                    "expected": "Returns a non‑null JsonSchema instance loaded from the HTTP resource. The meta‑schema version matches the $schema keyword present in the remote file.",
                    "description": "Ensures that the factory can resolve a SchemaLocation that uses the HTTP scheme, fetch the content over the network, and parse it correctly."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - YAML format support",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/yaml-schema.yaml"
                        }
                    ],
                    "expected": "Returns a non‑null JsonSchema instance whose structure matches the YAML definition. No exception is thrown, confirming that the factory auto‑detects YAML input format.",
                    "description": "Tests that a schema stored in YAML (instead of JSON) is correctly read by the factory via the default YamlMapper."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - unknown $schema version fallback",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/unknown-meta-schema.json"
                        }
                    ],
                    "expected": "Returns a JsonSchema instance using the library's default meta‑schema (latest supported draft). No exception is thrown even though the $schema IRI is not recognized.",
                    "description": "Validates that when a schema declares an unknown or unsupported $schema IRI, the factory falls back to the default meta‑schema rather than failing outright."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - cyclic $ref detection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/cyclic-schema.json"
                        }
                    ],
                    "expected": "Throws a JsonSchemaException (or a subclass) with a message indicating a cyclic $ref resolution was detected.",
                    "description": "Provides a schema where definition A $ref's B and B $ref's A. The test confirms that the loader detects the cycle and aborts with a clear exception rather than entering infinite recursion."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - relative $ref with $id resolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/relative-ref-schema.json"
                        }
                    ],
                    "expected": "Returns a JsonSchema where the relative $ref is correctly resolved against the base URI defined by the $id keyword in the same document.",
                    "description": "Ensures that the factory respects the $id base URI for relative reference resolution, a subtle but required part of the JSON‑Schema spec."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - custom SchemaLoader via builder",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/custom-loader-schema.json"
                        }
                    ],
                    "expected": "The returned JsonSchema originates from the custom SchemaLoader supplied to the factory builder (e.g., a mock loader that returns a predefined JsonNode). The test asserts that the loader’s `loadSchema` method was invoked.",
                    "description": "Demonstrates that a user‑provided SchemaLoader can override the default loading mechanism, allowing injection of mocked or pre‑processed schemas."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - legacy draft‑04 meta‑schema handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/draft04-schema.json"
                        }
                    ],
                    "expected": "Returns a JsonSchema whose meta‑schema version is DraftV4 (the library’s representation of draft‑04). The $schema keyword \"http://json-schema.org/draft-04/schema#\" is correctly interpreted.",
                    "description": "Verifies that the factory still supports the older draft‑04 meta‑schema and maps it to the appropriate internal VersionFlag."
                }
            ]
        }
    ]
}