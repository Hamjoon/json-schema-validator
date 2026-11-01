{
    "group": "JsonSchemaFactory.getYamlMapper",
    "cases": [
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "custom ObjectMapper instance"
                }
            ],
            "expected": "returns the same custom ObjectMapper instance that was injected",
            "description": "When the JsonSchemaFactory instance has a non‑null yamlMapper field, getYamlMapper() must return that exact instance instead of delegating to YamlMapperFactory."
        },
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "null"
                }
            ],
            "expected": "returns YamlMapperFactory.getInstance()",
            "description": "When the yamlMapper field is null, getYamlMapper() should fall back to the singleton provided by YamlMapperFactory and return it."
        },
        {
            "input": [],
            "expected": "returned ObjectMapper is not null",
            "description": "Regardless of the branch taken, the method must never return null; verify that the returned mapper is a non‑null ObjectMapper."
        },
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "null"
                }
            ],
            "expected": "YamlMapperFactory.getInstance() is called exactly once",
            "description": "When yamlMapper is null, ensure that the static factory method is invoked (can be verified with a spy/mock of YamlMapperFactory)."
        }
    ]
}