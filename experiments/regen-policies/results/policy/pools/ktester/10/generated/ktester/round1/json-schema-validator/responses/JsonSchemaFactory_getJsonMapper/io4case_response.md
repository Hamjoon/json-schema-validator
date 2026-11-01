{
    "group": "JsonSchemaFactory#getJsonMapper - jsonMapper is null",
    "cases": [
        {
            "input": [],
            "expected": "returns a non‑null ObjectMapper instance provided by JsonMapperFactory.getInstance()",
            "description": "Create a JsonSchemaFactory using the public builder (or default constructor) without injecting an ObjectMapper, so the private field remains null. Calling getJsonMapper() should invoke the false branch of the ternary operator and return the singleton ObjectMapper from JsonMapperFactory. The test asserts that the result is not null and is an instance of com.fasterxml.jackson.databind.ObjectMapper."
        },
        {
            "input": [],
            "expected": "subsequent calls return the same ObjectMapper instance from JsonMapperFactory",
            "description": "Using the same factory instance as the previous case, invoke getJsonMapper() twice. Both calls must return the exact same ObjectMapper reference (==), confirming that the method does not create a new mapper on each invocation when the internal jsonMapper field is null."
        }
    ]
}