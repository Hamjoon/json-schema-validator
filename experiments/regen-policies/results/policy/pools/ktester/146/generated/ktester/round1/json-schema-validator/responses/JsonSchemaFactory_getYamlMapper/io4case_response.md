{
    "group": "JsonSchemaFactory.getYamlMapper - multiple invocations with null field",
    "cases": [
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "null"
                },
                {
                    "parameter": "number of calls",
                    "value": "2"
                }
            ],
            "expected": "YamlMapperFactory.getInstance() is invoked exactly twice and each call returns the same ObjectMapper instance (reference equality)",
            "description": "When the internal yamlMapper is null, each invocation of getYamlMapper() should delegate to YamlMapperFactory.getInstance(). This test calls the method twice and verifies that the static factory method is called once per invocation and that both returned mappers are the same instance."
        },
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "custom ObjectMapper spy"
                },
                {
                    "parameter": "number of calls",
                    "value": "3"
                }
            ],
            "expected": "Returned ObjectMapper is the exact custom spy instance on every call; YamlMapperFactory.getInstance() is never invoked",
            "description": "Ensures idempotence when a non‑null yamlMapper is present: multiple calls must always return the injected instance without delegating to the factory."
        }
    ]
  ,
    "group": "JsonSchemaFactory.getYamlMapper - exception handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "null"
                },
                {
                    "parameter": "YamlMapperFactory.getInstance() behavior",
                    "value": "throws RuntimeException('factory failure')"
                }
            ],
            "expected": "RuntimeException with message 'factory failure' propagates out of getYamlMapper()",
            "description": "Verifies that getYamlMapper() does not swallow exceptions thrown by YamlMapperFactory.getInstance() and forwards them to the caller."
        }
    ]
  ,
    "group": "JsonSchemaFactory.getYamlMapper - field immutability",
    "cases": [
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "custom ObjectMapper instance"
                }
            ],
            "expected": "After calling getYamlMapper(), the yamlMapper field still references the original custom instance (no mutation)",
            "description": "Checks that the getter does not alter the internal yamlMapper field; the reference before and after the call must be identical."
        }
    ]
  ,
    "group": "JsonSchemaFactory.getYamlMapper - concurrent access",
    "cases": [
        {
            "input": [
                {
                    "parameter": "yamlMapper field (via reflection)",
                    "value": "null"
                },
                {
                    "parameter": "threads",
                    "value": "10"
                }
            ],
            "expected": "All threads receive a non‑null ObjectMapper; YamlMapperFactory.getInstance() is invoked at least once (exact count may vary) but no race conditions or exceptions occur",
            "description": "Simulates concurrent calls to getYamlMapper() on the same factory instance when yamlMapper is null, ensuring thread‑safety and that each thread obtains a valid mapper."
        }
    ]
}