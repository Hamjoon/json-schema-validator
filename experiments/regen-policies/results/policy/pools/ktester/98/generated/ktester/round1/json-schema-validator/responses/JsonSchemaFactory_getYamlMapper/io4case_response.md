{
    "testCases": [
        {
            "group": "yamlMapper non‑null field immutability",
            "cases": [
                {
                    "input": [],
                    "expected": "the injected ObjectMapper instance remains unchanged after getYamlMapper() call",
                    "description": "Create a JsonSchemaFactory with a custom ObjectMapper injected into the private final field `yamlMapper`. After invoking `getYamlMapper()`, use reflection to read the `yamlMapper` field again and assert that it still references the original injected mapper (i.e., the method does not replace or modify the field)."
                },
                {
                    "input": [],
                    "expected": "the returned mapper is exactly the same instance as the injected mapper",
                    "description": "Verify that `getYamlMapper()` returns the exact reference that was injected, not a copy or wrapper. The test obtains the injected mapper via reflection, calls `getYamlMapper()`, and checks reference equality."
                }
            ]
        },
        {
            "group": "yamlMapper null field fallback guarantees",
            "cases": [
                {
                    "input": [],
                    "expected": "a non‑null ObjectMapper instance",
                    "description": "Force the private `yamlMapper` field to `null` (using reflection). Call `getYamlMapper()` and assert that the result is not `null` and is an instance of `com.fasterxml.jackson.databind.ObjectMapper`."
                },
                {
                    "input": [],
                    "expected": "the same instance returned by YamlMapperFactory.getInstance() even if YamlMapperFactory is mocked to return a specific stub",
                    "description": "Using a mocking framework capable of static mocking (e.g., PowerMock), stub `YamlMapperFactory.getInstance()` to return a pre‑created mock ObjectMapper. Set `yamlMapper` to `null` and call `getYamlMapper()`. Verify that the returned object is exactly the stubbed mock."
                }
            ]
        },
        {
            "group": "yamlMapper null field concurrency",
            "cases": [
                {
                    "input": [],
                    "expected": "identical ObjectMapper instances across concurrent threads",
                    "description": "Set the `yamlMapper` field to `null`. Spawn a pool of multiple threads (e.g., 10) where each thread invokes `getYamlMapper()` and stores the result in a thread‑safe collection. After all threads complete, assert that every collected instance is the same (reference equality) and equals `YamlMapperFactory.getInstance()`. This ensures the fallback path is thread‑safe and deterministic."
                }
            ]
        },
        {
            "group": "yamlMapper field absent but YamlMapperFactory returns null (defensive)",
            "cases": [
                {
                    "input": [],
                    "expected": "a non‑null ObjectMapper (fallback to a newly created ObjectMapper) or a NullPointerException depending on implementation",
                    "description": "Mock `YamlMapperFactory.getInstance()` to return `null` (if the mocking framework permits). With `yamlMapper` set to `null`, call `getYamlMapper()`. The test verifies that the method never propagates a `null` reference; it should either throw a clear `NullPointerException` or internally create a default `ObjectMapper`. The expected behavior is documented in the test description."
                }
            ]
        }
    ]
}