{
  "group": "JsonSchemaFactory.getSchema(String) - basic delegation tests",
  "cases": [
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
        }
      ],
      "expected": "A non‑null JsonSchema instance is returned. The schema should be parsed without errors and its root type should be OBJECT.",
      "description": "Pass a well‑formed JSON schema string. The method should delegate to the overload that creates a default SchemaValidatorsConfig and return a valid JsonSchema object."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": ""
        }
      ],
      "expected": "IllegalArgumentException (or JsonSchemaFactoryException) indicating that the schema content is empty or cannot be parsed.",
      "description": "Provide an empty string. The underlying parser cannot create a JsonNode, so the factory should raise an exception."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": null
        }
      ],
      "expected": "NullPointerException (or IllegalArgumentException) because the schema argument is null.",
      "description": "Pass a null reference. The method should fail fast, propagating a NPE or a clear illegal‑argument exception."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "   \t\n  "
        }
      ],
      "expected": "IllegalArgumentException (or JsonSchemaFactoryException) indicating that the schema content is blank.",
      "description": "Supply a string that contains only whitespace. The factory should treat it as invalid input and throw an exception."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"unknown\" } } }"
        }
      ],
      "expected": "JsonSchemaFactoryException (or a validation exception) because the schema contains an unknown type keyword.",
      "description": "Pass a syntactically valid JSON string that is semantically invalid for JSON Schema. The factory should attempt to parse it and then fail during schema creation, throwing an appropriate exception."
    }
  ]
}