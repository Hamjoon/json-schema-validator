{
  "testCases": [
    {
      "group": "Valid inputs – config with custom settings",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"string\", \"minLength\": 5 }"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true).setTypeLoose(true)"
            }
          ],
          "expected": "JsonSchema instance whose internal schema node equals the supplied JsonNode and whose config has failFast=true and typeLoose=true",
          "description": "When a well‑formed JsonNode is supplied together with a non‑default SchemaValidatorsConfig, the factory should propagate the custom settings into the created JsonSchema."
        }
      ]
    },
    {
      "group": "Valid inputs – array type schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" }, \"minItems\": 2 }"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance representing an array schema with the specified constraints",
          "description": "The method should accept a schema that describes an array and return a JsonSchema that mirrors those constraints."
        }
      ]
    },
    {
      "group": "Valid inputs – boolean schema true",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "true"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance whose internal schema node is the boolean true (represents a schema that always validates)",
          "description": "JSON Schema draft‑7 permits a boolean value as the whole schema; the factory must handle this case without throwing an exception."
        }
      ]
    },
    {
      "group": "Invalid input – jsonNode is a numeric primitive",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "42"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException indicating that the root schema node must be an object or boolean",
          "description": "A numeric root is not a valid JSON Schema; the factory should reject it early with an appropriate exception."
        }
      ]
    },
    {
      "group": "Valid inputs – schema containing $ref (no resolution)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$ref\": \"#/definitions/name\", \"definitions\": { \"name\": { \"type\": \"string\" } } }"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance that stores the supplied JsonNode (including the $ref) without attempting to resolve it at creation time",
          "description": "The factory should not resolve $ref during getSchema; it merely wraps the node, allowing later validation to handle reference resolution."
        }
      ]
    },
    {
      "group": "Valid inputs – nested definitions and $id",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$id\": \"http://example.com/schemas/person\", \"type\": \"object\", \"properties\": { \"address\": { \"$ref\": \"address.json\" } }, \"definitions\": { \"address\": { \"type\": \"object\", \"properties\": { \"city\": { \"type\": \"string\" } } } } }"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance whose internal schema node contains the $id, properties, and definitions exactly as supplied",
          "description": "Ensures that complex schemas with $id, nested definitions and external $ref are accepted and preserved by the factory."
        }
      ]
    },
    {
      "group": "Invalid input – jsonNode is an empty array",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "[]"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException because a JSON Schema root cannot be an array",
          "description": "An empty JSON array is not a valid schema root; the factory should reject it with a clear exception."
        }
      ]
    }
  ]
}