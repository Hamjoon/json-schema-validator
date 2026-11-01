{
    "testCases": [
        {
            "group": "JsonSchemaFactory.builder() return type",
            "cases": [
                {
                    "input": [],
                    "expected": "instance of JsonSchemaFactory.Builder",
                    "description": "Verify that the object returned by JsonSchemaFactory.builder() is of the expected Builder type."
                },
                {
                    "input": [],
                    "expected": "Builder class name equals \"JsonSchemaFactory$Builder\"",
                    "description": "Confirm that the fully‑qualified class name of the returned object matches the nested Builder class."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.builder() build() creates factory",
            "cases": [
                {
                    "input": [],
                    "expected": "non‑null JsonSchemaFactory instance",
                    "description": "Calling build() on a freshly obtained Builder must produce a usable JsonSchemaFactory object; the result must not be null."
                },
                {
                    "input": [],
                    "expected": "factory instance is of type JsonSchemaFactory",
                    "description": "The object returned by Builder.build() should be an instance of JsonSchemaFactory."
                },
                {
                    "input": [],
                    "expected": "two factories from two builders are distinct",
                    "description": "Create two Builder objects, invoke build() on each, and verify that the resulting JsonSchemaFactory instances are different (different references)."
                }
            ]
        },
        {
            "group": "JsonSchemaFactory.builder() concurrency safety",
            "cases": [
                {
                    "input": [],
                    "expected": "all Builder instances obtained from parallel threads are distinct",
                    "description": "Spawn N (e.g., 10) threads, each calling JsonSchemaFactory.builder() and storing the returned Builder. After all threads complete, assert that every stored Builder reference is unique, confirming thread‑safe fresh instance creation."
                },
                {
                    "input": [],
                    "expected": "no exceptions thrown during concurrent builder() calls",
                    "description": "During the concurrent execution described above, ensure that no thread experiences an exception (e.g., NullPointerException), demonstrating that builder() is safe under concurrent access."
                }
            ]
        }
    ]
}