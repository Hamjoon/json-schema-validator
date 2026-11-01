{
    "test_cases": [
        {
            "group": "Cache enabled – null schemaUri",
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
                    "expected": "NullPointerException",
                    "description": "When caching is enabled and the supplied SchemaLocation is null, ConcurrentHashMap.get(null) throws NullPointerException before any call to getMappedSchema. The method must propagate this exception."
                }
            ]
        },
        {
            "group": "Cache enabled – getMappedSchema returns null (NPE on withConfig)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/returns-null.json\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "With caching enabled, if getMappedSchema returns null, loadSchema attempts to invoke withConfig on a null reference, causing a NullPointerException. The test verifies that this exception is thrown."
                }
            ]
        },
        {
            "group": "Cache enabled – getMappedSchema throws unchecked exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/throws.json\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "Simulate a scenario where getMappedSchema throws an unchecked IllegalStateException (e.g., due to I/O failure). loadSchema should not catch it and must let the exception propagate."
                }
            ]
        },
        {
            "group": "Cache disabled – getMappedSchema throws unchecked exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "new SchemaLocation(\"http://example.com/throws-no-cache.json\")"
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "When caching is disabled, loadSchema directly returns the result of getMappedSchema. If getMappedSchema throws an unchecked IllegalArgumentException, the method must propagate it unchanged."
                }
            ]
        }
    ]
}