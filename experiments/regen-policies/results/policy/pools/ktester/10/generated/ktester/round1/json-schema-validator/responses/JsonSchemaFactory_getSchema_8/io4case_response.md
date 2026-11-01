{
  "testCases": [
    {
      "group": "Boolean Schema Nodes",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "booleanNode (JsonNode representing JSON literal true)"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose internal schemaNode is a BooleanNode representing true",
          "description": "A JSON Schema can be the boolean literal true which means any instance is valid. The factory should accept a BooleanNode and return a JsonSchema without throwing."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "booleanNode (JsonNode representing JSON literal false)"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose internal schemaNode is a BooleanNode representing false",
          "description": "A JSON Schema can be the boolean literal false which means no instance is valid. The factory should accept a BooleanNode and return a JsonSchema without throwing."
        }
      ]
    },
    {
      "group": "Non‑Object/Array/Null/Boolean Nodes – TextNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "textNode (JsonNode representing a JSON string \"some text\")"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose internal schemaNode is a TextNode",
          "description": "Although a TextNode is not a valid schema according to the spec, the factory method does not perform validation at creation time; it should still wrap the node in a JsonSchema object."
        }
      ]
    },
    {
      "group": "Config Null with Non‑Empty Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"integer\", \"minimum\": 0 }"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchema instance (non‑null) created with internal default SchemaValidatorsConfig and the supplied integer schema",
          "description": "When the configuration argument is null the factory must fall back to its internal default configuration while still correctly handling a non‑empty schema node."
        }
      ]
    },
    {
      "group": "Custom Config with Multiple Flags",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"string\", \"format\": \"email\" }"
            },
            {
              "parameter": "config",
              "value": "customConfig { \"typeLoose\": true, \"format\": \"skip\", \"failFast\": true }"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose effective config has typeLoose=true, format validation disabled, and failFast=true",
          "description": "Ensures that a complex SchemaValidatorsConfig with several flags is fully propagated to the created JsonSchema."
        }
      ]
    },
    {
      "group": "Shared Config Isolation Across Calls",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"string\" }"
            },
            {
              "parameter": "config",
              "value": "sharedConfig { \"typeLoose\": false }"
            }
          ],
          "expected": "First JsonSchema instance reflects typeLoose=false; second call with same config after first call still reflects typeLoose=false (no mutation)",
          "description": "Pass the same SchemaValidatorsConfig instance to two successive getSchema calls and verify that the factory does not mutate the config between calls."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"number\" }"
            },
            {
              "parameter": "config",
              "value": "sharedConfig { \"typeLoose\": false }"
            }
          ],
          "expected": "Second JsonSchema instance also reflects typeLoose=false",
          "description": "Continuation of the previous scenario: the second schema creation must see the unchanged configuration."
        }
      ]
    },
    {
      "group": "Schema with $schema Identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"array\", \"items\": { \"type\": \"string\" } }"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose meta‑schema is Draft‑07 (as indicated by $schema) and whose internal schemaNode matches the supplied node",
          "description": "When the supplied schema contains a $schema keyword, the factory should load the corresponding meta‑schema and associate it with the created JsonSchema."
        }
      ]
    }
  ]
}