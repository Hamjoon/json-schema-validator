{
  "testCases": [
    {
      "group": "Successful schema parsing",
      "cases": [
        {
          "input": [
            { "parameter": "schema", "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}" },
            { "parameter": "inputFormat", "value": "JSON" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "returns a non‑null JsonSchema instance whose internal JsonNode matches the parsed schema",
          "description": "Valid JSON schema string with explicit JSON InputFormat should be parsed without error and a JsonSchema object created."
        },
        {
          "input": [
            { "parameter": "schema", "value": "type: object\nproperties:\n  age:\n    type: integer" },
            { "parameter": "inputFormat", "value": "YAML" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "returns a non‑null JsonSchema instance (YAML is correctly converted to JsonNode)",
          "description": "Valid YAML schema string with InputFormat.YAML should be parsed and wrapped in a JsonSchema."
        },
        {
          "input": [
            { "parameter": "schema", "value": "{\"type\":\"string\"}" },
            { "parameter": "inputFormat", "value": "null" },
            { "parameter": "config", "value": "null" }
          ],
          "expected": "returns a non‑null JsonSchema instance (default format used, default config applied)",
          "description": "When InputFormat and config are null the method should fall back to default handling and still produce a JsonSchema."
        }
      ]
    },
    {
      "group": "IOException handling (exception branch)",
      "cases": [
        {
          "input": [
            { "parameter": "schema", "value": "this is not a valid json nor yaml" },
            { "parameter": "inputFormat", "value": "JSON" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "throws JsonSchemaException wrapping the original IOException",
          "description": "Malformed schema string forces readTree to throw IOException; the method must catch it, log the error and re‑throw as JsonSchemaException."
        },
        {
          "input": [
            { "parameter": "schema", "value": "" },
            { "parameter": "inputFormat", "value": "JSON" },
            { "parameter": "config", "value": "new SchemaValidatorsConfig()" }
          ],
          "expected": "throws JsonSchemaException (empty content cannot be parsed into a JsonNode)",
          "description": "An empty string leads to an IOException from the underlying parser, exercising the catch block."
        }
      ]
    }
  ]
}