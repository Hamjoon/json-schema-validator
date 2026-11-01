{
    "newTestCases": [
        {
            "group": "Cache disabled - null config",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockSchemaLocationNullConfig"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "returns the JsonSchema instance produced by getMappedSchema when config is null (no caching performed)",
                    "description": "When enableSchemaCache is false and the supplied SchemaValidatorsConfig is null, loadSchema must delegate directly to getMappedSchema passing the null config and return whatever getMappedSchema returns."
                }
            ]
        },
        {
            "group": "Cache enabled - null config, cache miss",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockSchemaLocationNullConfigMiss"
                        },
                        {
                            "parameter": "config",
                            "value": null
                        }
                    ],
                    "expected": "calls getMappedSchema, stores the non‑null schema in the cache, invokes withConfig(null) on the cached schema and returns the result",
                    "description": "With enableSchemaCache true, an empty cache and a null config, loadSchema should obtain a schema via getMappedSchema, cache it, call withConfig(null) on the cached schema and return the value returned by withConfig."
                }
            ]
        },
        {
            "group": "Cache enabled - null schemaUri",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": null
                        },
                        {
                            "parameter": "config",
                            "value": "mockConfigX"
                        }
                    ],
                    "expected": "throws NullPointerException",
                    "description": "Passing a null SchemaLocation should cause a NullPointerException because the method accesses the map with the key without a null‑check."
                }
            ]
        },
        {
            "group": "Cache enabled - cache hit with withConfig returning new instance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockSchemaLocationHitWithConfig"
                        },
                        {
                            "parameter": "config",
                            "value": "mockConfigY"
                        }
                    ],
                    "expected": "returns the JsonSchema instance produced by cachedSchema.withConfig(mockConfigY) (a different instance than the cached one)",
                    "description": "When a schema is already cached, loadSchema must call withConfig on the cached schema and return the object that withConfig returns, even if that object is a new instance."
                }
            ]
        },
        {
            "group": "Cache enabled - cache miss, getMappedSchema throws runtime exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaUri",
                            "value": "mockSchemaLocationException"
                        },
                        {
                            "parameter": "config",
                            "value": "mockConfigZ"
                        }
                    ],
                    "expected": "propagates the RuntimeException thrown by getMappedSchema",
                    "description": "If getMappedSchema throws a RuntimeException during a cache‑miss scenario, loadSchema must not swallow the exception; it should propagate it to the caller."
                }
            ]
        }
    ]
}