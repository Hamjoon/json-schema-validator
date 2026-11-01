{
    "test_cases": [
        {
            "group": "metaSchemaFactory is non‑null – factory throws IllegalArgumentException for malformed IRI",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"not a valid uri\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException propagated",
                    "description": "The injected JsonMetaSchemaFactory mock is configured to throw IllegalArgumentException when getMetaSchema is called with a malformed IRI. The test verifies that loadMetaSchema does not catch the exception and the same IllegalArgumentException bubbles up to the caller."
                }
            ]
        },
        {
            "group": "metaSchemaFactory is null – DefaultJsonMetaSchemaFactory singleton is null (NPE)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/meta\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException propagated",
                    "description": "Using reflection the test replaces the internal singleton instance returned by DefaultJsonMetaSchemaFactory.getInstance() with null. When loadMetaSchema is invoked with metaSchemaFactory == null, the method attempts to call getMetaSchema on a null reference, causing a NullPointerException which should be propagated."
                }
            ]
        },
        {
            "group": "metaSchemaFactory is null – default factory throws IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"unsupported:meta\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException propagated",
                    "description": "The DefaultJsonMetaSchemaFactory singleton is replaced (via reflection) with a mock whose getMetaSchema method throws IllegalArgumentException for an unsupported IRI. The test ensures loadMetaSchema does not swallow the exception and the IllegalArgumentException reaches the caller."
                }
            ]
        },
        {
            "group": "metaSchemaFactory is non‑null – factory throws NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "iri",
                            "value": "\"http://example.com/nullptr\""
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException propagated",
                    "description": "A custom JsonMetaSchemaFactory mock is injected that deliberately throws a NullPointerException inside its getMetaSchema implementation. The test confirms that loadMetaSchema forwards this NullPointerException unchanged."
                }
            ]
        }
    ]
}