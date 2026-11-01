{
    "testCases": [
        {
            "group": "Unsupported Scheme",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"ftp://example.com/schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IllegalArgumentException) indicating the scheme is not supported",
                    "description": "The factory should reject URIs with an unsupported scheme such as ftp and throw an appropriate exception."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"jar:file:/path/to/jarfile.jar!/schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) because the jar scheme is not handled by the default ResourceFetcher",
                    "description": "Verify that a URI using the jar scheme is not resolved and results in a clear error."
                }
            ]
        },
        {
            "group": "Network Failure",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"https://nonexistent.example.com/missing-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) caused by a connection timeout or host not found",
                    "description": "When an HTTP/HTTPS URI points to a host that cannot be reached, the loader should surface the network error as a JsonSchemaException."
                }
            ]
        },
        {
            "group": "YAML Schema Resource",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/yaml-schema.yaml\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; the content is parsed via the YAML mapper and the resulting JsonNode matches the YAML definition",
                    "description": "The factory must recognise a .yaml/.yml resource, use the YamlMapper, and produce a valid JsonSchema object."
                }
            ]
        },
        {
            "group": "URI with Encoded Characters",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/space%20in%20name.json\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; the URI is correctly decoded and the schema is loaded",
                    "description": "Test that percent‑encoded characters (e.g., spaces) in the URI path are handled properly."
                },
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/special%23char.json\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; the fragment delimiter encoded as %23 does not split the URI",
                    "description": "Ensure that an encoded ‘#’ inside the path does not get interpreted as a fragment separator."
                }
            ]
        },
        {
            "group": "Relative Classpath URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:schemas/relative-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; the relative path is resolved against the classpath root",
                    "description": "Validate that a classpath URI without a leading slash is interpreted as a relative resource and loaded correctly."
                }
            ]
        },
        {
            "group": "Missing $schema Declaration",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/no-meta-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; the factory falls back to the default meta‑schema (e.g., DraftV7) when $schema is absent",
                    "description": "When a schema does not declare a $schema field, the factory should apply the default meta‑schema version."
                }
            ]
        },
        {
            "group": "External $ref Resolution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/main-with-ref.json\")"
                        }
                    ],
                    "expected": "JsonSchema instance is returned; any $ref inside the main schema that points to another classpath resource is resolved successfully",
                    "description": "Confirm that the loader follows $ref references to other schemas located via URIs and incorporates them into the resulting JsonSchema."
                }
            ]
        },
        {
            "group": "URI with Authentication Info",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"https://user:pass@example.com/secure-schema.json\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) because the default ResourceFetcher does not handle embedded credentials",
                    "description": "Test that a URI containing user‑info triggers an error rather than silently ignoring the credentials."
                }
            ]
        },
        {
            "group": "Empty String URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"\")"
                        }
                    ],
                    "expected": "IllegalArgumentException (or JsonSchemaException) indicating that the URI is empty or invalid",
                    "description": "An empty string creates a URI with no scheme/path; the factory should reject it with a clear exception."
                }
            ]
        },
        {
            "group": "Directory URI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "java.net.URI.create(\"classpath:/schemas/dir/\")"
                        }
                    ],
                    "expected": "JsonSchemaException (or IOException) because the URI points to a directory rather than a file",
                    "description": "When the URI resolves to a directory, the loader must not attempt to parse it as a schema and should raise an appropriate error."
                }
            ]
        }
    ]
}