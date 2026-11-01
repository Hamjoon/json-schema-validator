{
    "group": "JsonSchemaFactory#getJsonMapper - JsonMapperFactory throws exception",
    "cases": [
        {
            "input": [],
            "expected": "IllegalStateException propagated from JsonMapperFactory.getInstance()",
            "description": "Mock the static method JsonMapperFactory.getInstance() to throw IllegalStateException. Create a JsonSchemaFactory instance where the internal jsonMapper field remains null (e.g., via the public builder). Invoking getJsonMapper() must not catch the exception; it should be re‑thrown, confirming that the method propagates any runtime exception originating from JsonMapperFactory."
        }
    ]
}