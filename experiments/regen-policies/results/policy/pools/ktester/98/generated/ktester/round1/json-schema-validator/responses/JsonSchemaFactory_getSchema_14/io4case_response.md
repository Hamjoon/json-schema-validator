Invalid control character at: line 12 column 59 (char 397)
{
  "group": "JsonSchemaFactory.getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig) - additional edge cases",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"[1,2,3]\")   // an ArrayNode instead of an ObjectNode"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) – the factory should accept any JsonNode type and create a JsonSchema object; validation of the schema will later fail because the root node is not an object, but getSchema itself must not throw.",
      "description": "jsonNode is an ArrayNode (invalid as a JSON Schema root). Verify that getSchema still returns a JsonSchema instance and does not throw an exception."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig() with failFast=true"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) whose internal SchemaValidatorsConfig reports failFast=true",
      "description": "Provide a custom configuration (failFast enabled) and verify that the created JsonSchema carries this configuration."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/parent-schema.json\")   // parent-schema contains $ref to child-schema.json"
        },
        {
          "parameter": "jsonNode",
          "value": "null"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) with all $ref references resolved (e.g., child-schema.json is loaded)",
      "description": "schemaUri points to a schema that references another schema via $ref. The factory must resolve the reference and return a fully‑loaded JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\", \\\"maxLength\\\":5}\")   // overrides the content of the located schema"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) whose root schema node equals the supplied jsonNode (i.e., the explicit JsonNode overrides the file content)",
      "description": "When both a SchemaLocation and a non‑null JsonNode are supplied, the JsonNode should take precedence as the schema definition."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/malformed-schema.json\")   // file contains invalid JSON"
        },
        {
          "parameter": "jsonNode",
          "value": "null"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "SchemaLoadingException (or IOException) indicating that the schema cannot be parsed",
      "description": "The schema file exists but contains malformed JSON/YAML. The factory should propagate a loading/parsing exception."
    },
    {
      "input": [
        {
          "parameter": "schemaUri",
          "value": "new SchemaLocation(\"classpath:/schema/example-schema.json\")"
        },
        {
          "parameter": "jsonNode",
          "value": "ObjectMapper().createObjectNode()   // explicitly an empty ObjectNode"
        },
        {
          "parameter": "config",
          "value": "null"
        }
      ],
      "expected": "instance of JsonSchema (non‑null) using the supplied empty ObjectNode as the schema source and default configuration",
      "description": "Explicitly pass an empty ObjectNode (instead of null) together with a null config to verify that the factory uses the empty node and falls back to the default configuration."
    }
  ]
}