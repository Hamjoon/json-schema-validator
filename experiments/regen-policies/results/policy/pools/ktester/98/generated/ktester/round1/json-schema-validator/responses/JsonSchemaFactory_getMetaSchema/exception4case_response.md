{
    "testCases": [
        {
            "group": "Load failure – unknown IRI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"urn:unknown:meta-schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown by loadMetaSchema because the IRI does not correspond to any built‑in meta‑schema.",
                    "description": "When the normalized IRI is syntactically valid but does not match any known meta‑schema, loadMetaSchema should reject it and propagate an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Load failure – resource not found",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/schema/missing-meta-schema.json\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "RuntimeException (or IllegalStateException) wrapping an IOException because the underlying resource cannot be located.",
                    "description": "If the IRI resolves to a location that cannot be read (e.g., file not on classpath), loadMetaSchema will attempt to open an InputStream, fail with IOException, and re‑throw it as an unchecked exception."
                }
            ]
        },
        {
            "group": "Load failure – malformed meta‑schema JSON",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/schema/bad-json-meta-schema.json\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "RuntimeException (e.g., JsonProcessingException wrapped in RuntimeException) thrown while parsing the meta‑schema content.",
                    "description": "The IRI points to a resource that exists but contains invalid JSON. The parsing step inside loadMetaSchema should raise a JsonProcessingException, which is wrapped and propagated as an unchecked exception."
                }
            ]
        },
        {
            "group": "Concurrent load exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/schema/concurrent-failure-meta-schema.json\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "All threads receive the same IllegalArgumentException (or RuntimeException) thrown by loadMetaSchema; the exception is propagated to each caller.",
                    "description": "When multiple threads invoke getMetaSchema simultaneously with an IRI that triggers a loading failure, computeIfAbsent must propagate the exception to every thread and must not swallow it or create partial entries."
                }
            ]
        }
    ]
}