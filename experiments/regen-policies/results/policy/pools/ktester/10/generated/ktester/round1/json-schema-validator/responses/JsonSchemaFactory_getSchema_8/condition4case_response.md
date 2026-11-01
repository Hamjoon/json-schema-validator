{
  "testCases": [
    {
      "group": "Null Parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": null
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "NullPointerException thrown because jsonNode is null",
          "description": "Verify that getSchema throws a NullPointerException when both arguments are null. The method delegates to newJsonSchema which expects a non‑null schema node."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": null
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "NullPointerException thrown because jsonNode is null",
          "description": "When only the JsonNode argument is null the factory should reject the call regardless of a provided configuration."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "emptyObjectNode"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "JsonSchema instance (non‑null) with default configuration applied",
          "description": "If the configuration argument is null the factory must fall back to its internal default SchemaValidatorsConfig while still creating a schema from the supplied empty JSON object."
        }
      ]
    },
    {
      "group": "Valid Parameters – Simple Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"string\" }"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose internal schemaNode equals the supplied node and whose validators config equals defaultConfig",
          "description": "Happy‑path test: a minimal valid JSON schema and a default configuration should produce a usable JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"integer\" } } }"
            },
            {
              "parameter": "config",
              "value": "customConfig { \"typeLoose\": true }"
            }
          ],
          "expected": "JsonSchema instance (non‑null) with customConfig applied (e.g., typeLoose flag true) and containing the complex schema structure",
          "description": "Ensures that a non‑default SchemaValidatorsConfig is correctly propagated to the created JsonSchema."
        }
      ]
    },
    {
      "group": "Edge Cases – Special JsonNode Types",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "nullNode (JsonNode representing JSON null)"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose internal schemaNode is a NullNode",
          "description": "A JSON schema that is the literal `null` is still a valid schema according to the specification; the factory should accept a NullNode."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "arrayNode [ { \"type\": \"string\" }, { \"type\": \"number\" } ]"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "JsonSchema instance (non‑null) with an array schema node",
          "description": "Validate that the factory can handle a top‑level array node (used for schema combinators like anyOf) without throwing."
        }
      ]
    },
    {
      "group": "Configuration Isolation",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"string\" }"
            },
            {
              "parameter": "config",
              "value": "configA { \"typeLoose\": false }"
            }
          ],
          "expected": "JsonSchema instance whose effective config has typeLoose = false",
          "description": "Confirm that the config supplied to getSchema is not altered internally; the resulting schema must reflect the exact settings of configA."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "validSchemaNode { \"type\": \"string\" }"
            },
            {
              "parameter": "config",
              "value": "configB { \"typeLoose\": true }"
            }
          ],
          "expected": "JsonSchema instance whose effective config has typeLoose = true",
          "description": "Same as previous case but with a different flag value, ensuring the factory does not cache or reuse a previous configuration."
        }
      ]
    }
  ]
}