{
    "group": "JsonSchemaFactory#getJsonMapper - jsonMapper is non‑null",
    "cases": [
        {
            "input": [
                {
                    "parameter": "jsonMapper",
                    "value": "new ObjectMapper()"
                }
            ],
            "expected": "returns the same ObjectMapper instance that was injected",
            "description": "Create a JsonSchemaFactory instance with a non‑null ObjectMapper (using reflection to set the private final field). The call to getJsonMapper() must return that exact instance, exercising the true branch of the ternary operator."
        },
        {
            "input": [
                {
                    "parameter": "jsonMapper",
                    "value": "new ObjectMapper()"
                }
            ],
            "expected": "subsequent calls return the same injected instance",
            "description": "Invoke getJsonMapper() twice on the same factory instance. Both calls should return the same injected ObjectMapper, confirming that the method does not create a new mapper each time."
        }
    ]
}