{
    "testCases": [
        {
            "group": "yamlMapper fallback throws runtime exception",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException propagated from YamlMapperFactory.getInstance()",
                    "description": "Force the private `yamlMapper` field to `null`. Use a static‑mocking framework (e.g., PowerMock, Mockito inline) to make `YamlMapperFactory.getInstance()` throw a `RuntimeException` (or a specific subclass such as `IllegalStateException`). Invoke `getYamlMapper()` and verify that the same exception bubbles up unchanged."
                }
            ]
        },
        {
            "group": "yamlMapper fallback returns null (unexpected null handling)",
            "cases": [
                {
                    "input": [],
                    "expected": "null returned (no exception thrown by getYamlMapper())",
                    "description": "Set the private `yamlMapper` field to `null` and mock `YamlMapperFactory.getInstance()` to return `null`. Call `getYamlMapper()` and assert that the method returns `null` rather than throwing an exception. This documents the edge case where the fallback provider itself is mis‑configured."
                },
                {
                    "input": [],
                    "expected": "NullPointerException when the returned mapper is used",
                    "description": "After the previous setup (fallback returns `null`), attempt to call a method on the returned `ObjectMapper` (e.g., `writeValueAsString`). The call should raise a `NullPointerException`, confirming that the factory does not guard against a `null` fallback."
                }
            ]
        }
    ]
}