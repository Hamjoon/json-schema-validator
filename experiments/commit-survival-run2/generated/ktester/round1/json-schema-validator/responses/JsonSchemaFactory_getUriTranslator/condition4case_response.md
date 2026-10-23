{
    "tests": [
        {
            "group": "DefaultFactory",
            "cases": [
                {
                    "input": [],
                    "expected": "URITranslator that performs identity translation (input URI equals output URI)",
                    "description": "Create a JsonSchemaFactory using the default builder (no custom uriMap or uriTranslators). Call getUriTranslator() and verify that the returned translator returns the same URI that is passed to its translate(URI) method, because the default uriMap is empty."
                }
            ]
        },
        {
            "group": "CustomUriMap_WithSingleEntry",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "sourceUri",
                            "value": "http://example.com/schema"
                        },
                        {
                            "parameter": "targetUri",
                            "value": "classpath:/schemas/example-schema.json"
                        }
                    ],
                    "expected": "URITranslator that maps http://example.com/schema to classpath:/schemas/example-schema.json",
                    "description": "Build a JsonSchemaFactory with a custom uriMap containing a single mapping. After calling getUriTranslator(), invoke translate(sourceUri) and assert that the result equals targetUri. This covers the branch where uriMap is non‑empty."
                }
            ]
        },
        {
            "group": "CustomUriMap_WithMultipleEntries",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "mappings",
                            "value": {
                                "http://a.com": "classpath:/a.json",
                                "http://b.com": "classpath:/b.json"
                            }
                        },
                        {
                            "parameter": "testUri",
                            "value": "http://b.com"
                        }
                    ],
                    "expected": "URITranslator that maps http://b.com to classpath:/b.json",
                    "description": "Create a factory with a uriMap that has several entries. Verify that the translator correctly selects the mapping for the requested key, ensuring the map lookup logic works for more than one entry."
                }
            ]
        },
        {
            "group": "EmptyUriMap",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "uri",
                            "value": "http://no.mapping/uri"
                        }
                    ],
                    "expected": "URITranslator that returns the original URI (identity) because the map is empty",
                    "description": "Build a factory with an explicitly empty uriMap (Collections.emptyMap()). The translator should behave as an identity translator when no mapping exists."
                }
            ]
        },
        {
            "group": "NullUriMap",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException thrown when getUriTranslator() is invoked",
                    "description": "Using reflection (or a custom Builder stub) set the private field uriMap to null. Calling getUriTranslator() should throw NPE because URITranslator.map(null) is not allowed. This tests the defensive‑programming branch where uriMap is unexpectedly null."
                }
            ]
        },
        {
            "group": "NullCompositeURITranslator",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException thrown when getUriTranslator() is invoked",
                    "description": "Manipulate the private field uriTranslators to be null (e.g., via reflection). The method attempts to call with() on a null reference, resulting in NPE. This verifies behavior when the composite translator is not initialized."
                }
            ]
        }
    ]
}