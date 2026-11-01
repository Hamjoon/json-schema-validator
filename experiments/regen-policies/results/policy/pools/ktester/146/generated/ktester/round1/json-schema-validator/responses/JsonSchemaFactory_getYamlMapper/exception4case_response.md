{
    "tests": [
        {
            "group": "JsonSchemaFactory.getYamlMapper - invalid yamlMapper type",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "yamlMapper field (via reflection)",
                            "value": "String instance (\"not an ObjectMapper\")"
                        }
                    ],
                    "expected": "ClassCastException",
                    "description": "When the yamlMapper field is non‑null but holds an object that is not an instance of ObjectMapper, the ternary operator returns that value and the JVM throws a ClassCastException at the return statement."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.getYamlMapper - YamlMapperFactory throws",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "yamlMapper field (via reflection)",
                            "value": "null"
                        },
                        {
                            "parameter": "YamlMapperFactory.getInstance throws",
                            "value": "IllegalStateException(\"factory failure\")"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "When yamlMapper is null, getYamlMapper() delegates to YamlMapperFactory.getInstance(). If that static method throws a RuntimeException (simulated here as IllegalStateException), the exception must propagate unchanged."
                }
            ]
        }
    ]
}