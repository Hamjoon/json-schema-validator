{
    "test_cases": [
        {
            "group": "JsonMapperFactory.getInstance throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException is propagated from JsonMapperFactory.getInstance()",
                    "description": "Mock (e.g., with PowerMockito) the static method JsonMapperFactory.getInstance() to throw a RuntimeException. Create a JsonSchemaFactory instance with a null jsonMapper field and invoke getJsonMapper(). Verify that the same RuntimeException bubbles up, confirming that the method does not catch unchecked exceptions from the fallback factory."
                }
            ]
        },
        {
            "group": "JsonMapperFactory.getInstance throws Error",
            "cases": [
                {
                    "input": [],
                    "expected": "Error (e.g., NoClassDefFoundError) is propagated",
                    "description": "Configure the static JsonMapperFactory.getInstance() to throw an Error such as NoClassDefFoundError. Call getJsonMapper() on a factory whose jsonMapper field is null and assert that the Error propagates unchanged."
                }
            ]
        },
        {
            "group": "jsonMapper field access triggers exception via custom subclass",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException is propagated",
                    "description": "Create a subclass of JsonSchemaFactory (inside the same package) that overrides the jsonMapper field with a getter method that throws a RuntimeException when accessed (e.g., by using a custom ObjectMapper implementation whose method invoked during field read throws). Invoke getJsonMapper() and verify that the exception is thrown, demonstrating that any exception while reading the field is not swallowed."
                }
            ]
        }
    ]
}