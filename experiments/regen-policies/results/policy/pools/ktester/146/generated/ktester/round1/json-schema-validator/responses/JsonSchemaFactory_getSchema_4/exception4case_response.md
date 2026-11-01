{
    "test_cases": [
        {
            "group": "getSchema(SchemaLocation) - unsupported URI scheme",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "ftp://example.com/schemas/unsupported-schema.json"
                        }
                    ],
                    "expected": "Throws IllegalArgumentException indicating that the URI scheme is not supported by JsonSchemaFactory.",
                    "description": "Provides a SchemaLocation using an unsupported 'ftp' scheme. The factory should reject it and raise an IllegalArgumentException before any network or file access."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - malformed URI string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "://bad-uri-format"
                        }
                    ],
                    "expected": "Throws IllegalArgumentException (or URISyntaxException wrapped) because the supplied string cannot be parsed into a valid URI.",
                    "description": "Supplies a syntactically invalid URI string. The method should detect the parsing failure early and throw an appropriate exception."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - unreadable file (IO error)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "file:/tmp/unreadable-schema.json"
                        }
                    ],
                    "expected": "Throws IOException (or JsonSchemaException wrapping IOException) indicating that the schema file could not be read due to permission issues.",
                    "description": "Points to a local file that exists but lacks read permissions. The factory attempts to open the stream and encounters an IOException, which should be propagated."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - empty schema content",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/empty-schema.json"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or JsonProcessingException) because the content is empty and cannot be parsed into a JsonNode.",
                    "description": "Loads a schema resource that exists but contains zero bytes. Parsing an empty document should fail and result in a JsonSchemaException."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - unresolved $ref target",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/unresolved-ref-schema.json"
                        }
                    ],
                    "expected": "Throws JsonSchemaException with a message indicating that a $ref could not be resolved because the target definition does not exist.",
                    "description": "The schema contains a $ref that points to a non‑existent definition within the same document or an external document. The loader should detect the missing target and raise an exception."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - invalid $schema IRI format",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/invalid-meta-iri-schema.json"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or IllegalArgumentException) because the $schema keyword contains a malformed IRI that cannot be normalized.",
                    "description": "The schema declares a $schema value that is not a valid IRI (e.g., missing scheme). The factory should validate the IRI format and fail with an appropriate exception."
                }
            ]
        },
        {
            "group": "getSchema(SchemaLocation) - binary file supplied as schema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "classpath:/schemas/binary-data.bin"
                        }
                    ],
                    "expected": "Throws JsonSchemaException (or JsonProcessingException) because the content is not valid JSON/YAML and cannot be parsed.",
                    "description": "Points to a binary resource that is not a textual JSON/YAML schema. The factory attempts to detect the format, fails, and throws a parsing‑related exception."
                }
            ]
        }
    ]
}