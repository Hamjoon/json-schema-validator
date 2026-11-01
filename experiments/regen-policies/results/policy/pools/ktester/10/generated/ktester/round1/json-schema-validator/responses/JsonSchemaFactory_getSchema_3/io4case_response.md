{
  "testCases": [
    {
      "group": "JsonSchemaFactory.getSchema(JsonNode) - boolean schema true",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"true\")"
            }
          ],
          "expected": "non‑null JsonSchema instance representing a true (always‑valid) schema",
          "description": "Pass a JSON boolean node `true` which is a valid JSON‑Schema representation; the factory should return a JsonSchema that validates any instance successfully."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(JsonNode) - boolean schema false",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"false\")"
            }
          ],
          "expected": "non‑null JsonSchema instance representing a false (always‑invalid) schema",
          "description": "Pass a JSON boolean node `false`; the factory must still create a JsonSchema object that will reject every instance."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(JsonNode) - $ref handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$ref\":\"#/definitions/foo\",\"definitions\":{\"foo\":{\"type\":\"string\"}}}')"
            }
          ],
          "expected": "non‑null JsonSchema instance whose resolved schema type is `string`",
          "description": "Provide a schema containing a `$ref` to an internal definition; the factory should resolve the reference and produce a usable JsonSchema."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(JsonNode) - custom meta‑schema version (draft‑07)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"null\"}')"
            }
          ],
          "expected": "non‑null JsonSchema instance whose meta‑schema version is Draft‑07",
          "description": "Supply a schema that explicitly declares `$schema` draft‑07; the factory must load the corresponding meta‑schema and associate it with the created JsonSchema."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(JsonNode) - unknown $schema URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree('{\"$schema\":\"http://example.com/unknown/schema#\",\"type\":\"object\"}')"
            }
          ],
          "expected": "non‑null JsonSchema instance falling back to the default meta‑schema",
          "description": "When the `$schema` URI cannot be resolved, the factory should not fail but revert to the default meta‑schema (usually the latest supported version)."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid node type (array)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"[]\")"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "An array node is not a valid top‑level JSON‑Schema representation; the factory is expected to reject it with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(JsonNode) - invalid node type (string)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper.readTree(\"\\\"just a string\\\"\")"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A plain string node cannot be interpreted as a JSON‑Schema; the method should throw IllegalArgumentException."
        }
      ]
    }
  ]
}