{
    "group": "JsonSchemaFactory.getJsonMapper() - non‑null jsonMapper branch",
    "cases": [
        {
            "input": [],
            "expected": "the same ObjectMapper instance that was supplied to the factory constructor",
            "description": "Create a JsonSchemaFactory instance whose private final field `jsonMapper` is initialized with a custom ObjectMapper (e.g., new ObjectMapper()). Calling getJsonMapper() should return that exact instance (reference equality). This covers the true branch of the ternary operator (`this.jsonMapper != null`)."
        }
    ]
}