{
  "group": "JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig) - basic behavior",
  "cases": [
    {
      "input": [
        { "parameter": "schema", "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }" },
        { "parameter": "config", "value": "null" }
      ],
      "expected": "non‑null JsonSchema instance whose root node matches the supplied JSON schema",
      "description": "Valid JSON schema string with default (null) configuration should be parsed and a JsonSchema object returned."
    },
    {
      "input": [
        { "parameter": "schema", "value": "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }" },
        { "parameter": "config", "value": "new SchemaValidatorsConfig().setFailFast(true)" }
      ],
      "expected": "non‑null JsonSchema instance whose ValidationContext is configured with failFast=true",
      "description": "Valid JSON schema string together with a custom SchemaValidatorsConfig (failFast enabled) should be honoured by the created schema."
    },
    {
      "input": [
        { "parameter": "schema", "value": "null" },
        { "parameter": "config", "value": "null" }
      ],
      "expected": "IllegalArgumentException",
      "description": "Passing a null schema string must result in an IllegalArgumentException (the factory cannot resolve a null location)."
    },
    {
      "input": [
        { "parameter": "schema", "value": "\"\"" },
        { "parameter": "config", "value": "null" }
      ],
      "expected": "IllegalArgumentException",
      "description": "An empty schema string is not a valid location; the method should reject it with an IllegalArgumentException."
    },
    {
      "input": [
        { "parameter": "schema", "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"unknown\" } } }" },
        { "parameter": "config", "value": "null" }
      ],
      "expected": "JsonSchemaException (or JsonProcessingException) indicating schema parsing failure",
      "description": "A malformed JSON schema (unknown type) should cause a parsing/validation exception during schema creation."
    },
    {
      "input": [
        { "parameter": "schema", "value": "http://example.com/schemas/person.json" },
        { "parameter": "config", "value": "null" }
      ],
      "expected": "non‑null JsonSchema instance loaded from the remote URI (loader invoked)",
      "description": "When the schema string is an absolute URI, the factory must resolve it via the SchemaLoader and return the corresponding JsonSchema."
    },
    {
      "input": [
        { "parameter": "schema", "value": "http://json-schema.org/draft-07/schema#" },
        { "parameter": "config", "value": "null" }
      ],
      "expected": "non‑null JsonSchema representing the Draft‑07 meta‑schema",
      "description": "A known meta‑schema identifier should be recognised and the factory should return the built‑in meta‑schema object."
    }
  ]
}