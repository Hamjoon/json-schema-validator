{
  "test_cases": [
    {
      "group": "Null JsonNode Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "When getSchema is called with a null JsonNode the method should throw a NullPointerException because the underlying newJsonSchema implementation expects a non‑null schema node."
        }
      ]
    },
    {
      "group": "Empty Object Node",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().createObjectNode()"
            }
          ],
          "expected": "JsonSchema instance (non‑null) with no validation errors",
          "description": "An empty JSON object represents an empty schema. The factory should return a valid JsonSchema object (not null) that can be used for validation without throwing any exception."
        }
      ]
    },
    {
      "group": "Simple Valid Schema Node",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"type\":\"string\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose getSchemaNode() equals the supplied node",
          "description": "A minimal valid JSON‑Schema (type string) should be accepted and wrapped in a JsonSchema object. The returned schema should contain the same node that was supplied."
        }
      ]
    },
    {
      "group": "Schema Node with $schema Keyword (different meta‑schema versions)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"integer\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) that uses Draft‑07 meta‑schema internally",
          "description": "When the supplied node contains a $schema keyword the factory must detect the version (draft‑07) and create a JsonSchema that validates against that meta‑schema."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"type\":\"boolean\"}')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) that uses Draft‑04 meta‑schema internally",
          "description": "Same as above but for draft‑04. This verifies that the internal version‑selection branch is exercised."
        }
      ]
    },
    {
      "group": "Invalid Schema Node (syntax error)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('{\"type\":123}')"
            }
          ],
          "expected": "JsonSchemaValidationException (or any RuntimeException thrown by newJsonSchema)",
          "description": "A schema where the 'type' keyword has an illegal value should cause the factory to reject the schema and throw an exception during schema creation."
        }
      ]
    },
    {
      "group": "Non‑Object JsonNode (e.g., array)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree('[{\"type\":\"string\"}]')"
            }
          ],
          "expected": "JsonSchema instance (non‑null) that treats the array as the root schema node",
          "description": "The factory should accept any JsonNode, not only objects. Passing an array verifies the branch that handles non‑object root nodes."
        }
      ]
    }
  ]
}