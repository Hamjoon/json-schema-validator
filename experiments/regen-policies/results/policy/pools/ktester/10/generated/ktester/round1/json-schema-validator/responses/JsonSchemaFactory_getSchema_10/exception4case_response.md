{
    "group": "Cache Enabled – getMappedSchema Returns Null",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new TestSchemaLocation(\"http://example.com/null-schema\")"
                },
                {
                    "parameter": "config",
                    "value": "new SchemaValidatorsConfig()"
                }
            ],
            "expected": "NullPointerException is thrown because getMappedSchema returns null and the subsequent withConfig(config) call is invoked on a null reference.",
            "description": "When caching is enabled and the underlying getMappedSchema method returns null (e.g., schema not found), the factory attempts to call withConfig on the null result, leading to an NPE."
        }
    ]
}