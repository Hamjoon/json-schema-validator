{
  "group": "BaseJsonValidator.getSchemaLocation",
  "cases": [
    {
      "input": [],
      "expected": "null",
      "description": "When the BaseJsonValidator instance is created with a null schemaLocation, getSchemaLocation() should return null."
    },
    {
      "input": [],
      "expected": "SchemaLocation instance with URI \"http://example.com/schema\"",
      "description": "When the validator is initialized with a concrete SchemaLocation (e.g., new SchemaLocation(\"http://example.com/schema\")), getSchemaLocation() must return the exact same instance."
    },
    {
      "input": [],
      "expected": "the same mock SchemaLocation object that was injected",
      "description": "Using a mocked SchemaLocation (e.g., Mockito mock), verify that getSchemaLocation() returns the mock instance, confirming that the method does not create a new object or alter the reference."
    }
  ]
}