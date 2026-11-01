{
    "test_cases": [
        {
            "group": "Invalid schema content – malformed JSON",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "URI.create(\"classpath:/schemas/malformed-json-schema.json\")"
                        }
                    ],
                    "expected": "JsonProcessingException (or SchemaLoadingException wrapping it) indicating JSON parsing failure",
                    "description": "The URI points to a classpath resource that contains syntactically invalid JSON. The factory should attempt to read the content, fail during parsing, and propagate a parsing‑related exception."
                }
            ]
        },
        {
            "group": "Invalid schema content – malformed YAML",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "URI.create(\"classpath:/schemas/malformed-yaml-schema.yaml\")"
                        }
                    ],
                    "expected": "JsonProcessingException (or SchemaLoadingException wrapping it) indicating YAML parsing failure",
                    "description": "The URI points to a YAML schema file with incorrect indentation / syntax. The factory must detect the .yaml extension, use the YAML mapper, and surface the parsing error."
                }
            ]
        },
        {
            "group": "Schema with invalid $ref – referenced schema not found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "URI.create(\"classpath:/schemas/schema-with-bad-ref.json\")"
                        }
                    ],
                    "expected": "SchemaLoadingException (or RuntimeException) reporting that the $ref target cannot be resolved",
                    "description": "The provided schema contains a $ref that points to a non‑existent schema file. During loading the factory should attempt to resolve the reference and throw an exception when the target cannot be located."
                }
            ]
        },
        {
            "group": "Schema with unsupported meta‑schema IRI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "URI.create(\"classpath:/schemas/schema-with-unknown-meta.json\")"
                        }
                    ],
                    "expected": "IllegalArgumentException (or SchemaLoadingException) indicating that the meta‑schema IRI is not recognized",
                    "description": "The schema declares an \"$schema\" attribute whose IRI does not correspond to any known JsonMetaSchema. The factory should fail while trying to load the meta‑schema."
                }
            ]
        },
        {
            "group": "IO error – unreadable file (permission denied)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "URI.create(\"file:/tmp/unreadable-schema.json\")"
                        }
                    ],
                    "expected": "IOException (or SchemaLoadingException wrapping it) caused by access‑permission failure",
                    "description": "The file exists but the process lacks read permissions. When the factory attempts to open the InputStream, an IOException should be thrown and propagated."
                }
            ]
        },
        {
            "group": "Circular reference detection – recursion limit exceeded",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "URI.create(\"classpath:/schemas/circular-ref-schema.json\")"
                        }
                    ],
                    "expected": "StackOverflowError or SchemaLoadingException indicating circular reference / recursion depth exceeded",
                    "description": "The schema contains a $ref chain that eventually references itself, creating a circular dependency. The factory must detect the recursion limit and raise an appropriate exception rather than entering an infinite loop."
                }
            ]
        }
    ]
}