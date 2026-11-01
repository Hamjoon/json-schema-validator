{
    "group": "JsonSchemaFactory#getYamlMapper",
    "cases": [
        {
            "input": [],
            "expected": "Returns the injected yamlMapper instance (same object that was set in the factory)",
            "description": "When the JsonSchemaFactory is constructed with a non‑null yamlMapper, getYamlMapper() should return that exact instance, exercising the true branch of the ternary expression."
        },
        {
            "input": [],
            "expected": "Returns YamlMapperFactory.getInstance() (a non‑null ObjectMapper different from any injected one)",
            "description": "When the JsonSchemaFactory is constructed without a yamlMapper (field remains null), getYamlMapper() should fall back to YamlMapperFactory.getInstance(), exercising the false branch of the ternary expression."
        }
    ]
}