{
    "group": "JsonSchemaFactory#getYamlMapper - fallback factory exception",
    "cases": [
        {
            "input": [],
            "expected": "IllegalStateException propagated from YamlMapperFactory.getInstance()",
            "description": "When the factory's yamlMapper field is null and YamlMapperFactory.getInstance() throws an IllegalStateException (simulated via static mocking), getYamlMapper() must not catch the exception and should let it propagate to the caller."
        }
    ]
}