{
  "group": "JsonSchemaFactory.getSchema(String)",
  "cases": [
    {
      "input": [
        {
          "parameter": "schema",
          "value": null
        }
      ],
      "expected": "IllegalArgumentException (or NullPointerException) because the schema string is null",
      "description": "Verify that passing a null argument is rejected – the method should not attempt to load a schema and must throw an exception immediately."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": ""
        }
      ],
      "expected": "IllegalArgumentException because an empty string cannot be resolved to a schema location",
      "description": "Check the guard that validates the input string is not empty before any loading logic is executed."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema is the default (Draft‑07) and which validates the supplied JSON structure",
      "description": "Supply a minimal valid JSON schema as a raw string. The method should parse it, create a JsonSchema object and return it without trying to resolve an external URI."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "classpath:/schemas/simple-schema.json"
        }
      ],
      "expected": "non‑null JsonSchema instance loaded from the class‑path resource",
      "description": "Pass a class‑path location. The implementation should recognise the prefix, load the resource via the class‑loader and return the parsed schema."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "http://example.com/schemas/remote-schema.json"
        }
      ],
      "expected": "non‑null JsonSchema instance loaded from the remote HTTP URI",
      "description": "Provide a URL. The method must treat the string as a URI, use the configured SchemaLoader to fetch the content and return the resulting JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "file:/tmp/local-schema.json"
        }
      ],
      "expected": "non‑null JsonSchema instance loaded from the file system",
      "description": "Test a file‑URI scheme. The factory should resolve the file location, read the JSON content and produce a schema object."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "---\ntype: object\nproperties:\n  name:\n    type: string\n"
        }
      ],
      "expected": "JsonProcessingException (or IOException) because the default input format is JSON and the content is YAML",
      "description": "Supply a YAML document while the overload assumes JSON. The parser should fail, exposing the branch that validates the input format."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"object\" }"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema is Draft‑04 (detected via the $schema keyword)",
      "description": "The schema string explicitly declares a different meta‑schema version. The method must detect the $schema attribute, load the corresponding meta‑schema and use it for validation."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"unknown\" } } }"
        }
      ],
      "expected": "JsonSchemaCreationException (or a validation exception) because the schema contains an unknown type",
      "description": "Even though the string is syntactically valid JSON, the content is semantically invalid. The branch that validates the parsed JsonNode against the meta‑schema should raise an error."
    },
    {
      "input": [
        {
          "parameter": "schema",
          "value": "   { \"type\": \"object\" }   "
        }
      ],
      "expected": "non‑null JsonSchema instance (whitespace trimmed correctly)",
      "description": "Verify that leading/trailing whitespace does not affect schema parsing – the method should trim or otherwise handle it before delegating to the parser."
    }
  ]
}