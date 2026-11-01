{
    "testCases": [
        {
            "group": "Config variance does not affect caching",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/schema\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "first call returns a JsonMetaSchema created by loadMetaSchema; second call with a DIFFERENT non‑null config returns the SAME cached JsonMetaSchema instance and loadMetaSchema is NOT invoked the second time",
                    "description": "The cache key is derived solely from the normalized IRI. Supplying a different SchemaValidatorsConfig on the second invocation must not cause a new meta‑schema to be loaded; the previously cached instance is returned."
                }
            ]
        },
        {
            "group": "Case‑insensitive IRI normalization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"HTTP://EXAMPLE.COM/SCHEMA\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "first call loads a JsonMetaSchema (loader invoked); second call with \"http://example.com/schema\" returns the SAME instance and loader is not invoked again",
                    "description": "If normalizeMetaSchemaUri lower‑cases the IRI, two textual representations that differ only by case should map to the same cache entry, proving case‑insensitive normalization."
                }
            ]
        },
        {
            "group": "Exception propagation from loadMetaSchema",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/bad\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "throws the RuntimeException (or IOException wrapped) that loadMetaSchema propagates; no entry is added to the cache for the bad IRI",
                    "description": "A subclass of JsonSchemaFactory overrides loadMetaSchema to throw a deliberate exception. The test verifies that getMetaSchema does not swallow the exception and that the cache remains unchanged after the failure."
                }
            ]
        },
        {
            "group": "Concurrent loading results in single loader invocation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/concurrent\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "all threads receive the same JsonMetaSchema instance; loadMetaSchema is invoked exactly once despite N concurrent calls",
                    "description": "Using multiple threads that simultaneously call getMetaSchema with the same IRI, the test asserts that the ConcurrentMap.computeIfAbsent semantics guarantee a single execution of the loader lambda, preventing duplicate meta‑schema creation."
                }
            ]
        },
        {
            "group": "Cache size reflects distinct normalized IRIs",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/first\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "after invoking getMetaSchema with three distinct normalized IRIs (e.g., \"http://example.com/first\", \"http://example.com/second/\", \"HTTP://EXAMPLE.COM/THIRD\"), the internal metaSchemas map size equals 3",
                    "description": "The test calls getMetaSchema with three different IRIs that normalize to three distinct keys and then inspects the factory's private metaSchemas map (via reflection) to ensure the cache contains exactly three entries, confirming that normalization does not incorrectly collapse unrelated URIs."
                }
            ]
        }
    ]
}