{
  "tests": [
    {
      "group": "Relative URI – path resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "schemas/myschema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"schemas/myschema.json\" and whose root node equals the supplied JsonNode.",
          "description": "A relative URI (no scheme) should be accepted; the method forwards the string unchanged to SchemaLocation.of and returns a schema whose location matches the relative path."
        }
      ]
    },
    {
      "group": "URI with fragment and query parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schema.json?version=1#section2"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation exactly matches the full URI including query and fragment, and whose root node equals the supplied JsonNode.",
          "description": "Ensures that the method does not strip query strings or fragments; the full URI string is used as the schema location."
        }
      ]
    },
    {
      "group": "Complex nested schema node",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "https://example.org/complex-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\n  \"type\": \"object\",\n  \"properties\": {\n    \"address\": {\n      \"type\": \"object\",\n      \"properties\": {\n        \"street\": {\"type\": \"string\"},\n        \"city\": {\"type\": \"string\"}\n      },\n      \"required\": [\"street\", \"city\"]\n    },\n    \"phones\": {\n      \"type\": \"array\",\n      \"items\": {\"type\": \"string\", \"pattern\": \"^\\\\+?[0-9]+$\"}\n    }\n  },\n  \"required\": [\"address\"]\n}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"https://example.org/complex-schema.json\" and whose root node equals the supplied complex JsonNode.",
          "description": "Verifies that the factory can handle a deeply nested, realistic schema without altering its structure."
        }
      ]
    },
    {
      "group": "Non‑object JSON node – array schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/array-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"http://example.com/array-schema.json\" and whose root node equals the supplied JsonNode representing an array schema.",
          "description": "Checks that the method does not require the supplied JsonNode to be an object; array‑type schemas are accepted."
        }
      ]
    },
    {
      "group": "Non‑object JSON node – primitive schema (string)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/string-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"string\",\"maxLength\":10}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"http://example.com/string-schema.json\" and whose root node equals the supplied JsonNode representing a primitive string schema.",
          "description": "Ensures that a schema describing a primitive type (string) is passed through unchanged."
        }
      ]
    },
    {
      "group": "MissingNode as jsonNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/missing-node.json"
            },
            {
              "parameter": "jsonNode",
              "value": "MissingNode"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"http://example.com/missing-node.json\" and whose root node is a Jackson MissingNode (indicating a missing schema definition).",
          "description": "Jackson's MissingNode is a special JsonNode subclass; the factory should forward it to newJsonSchema without throwing."
        }
      ]
    },
    {
      "group": "Encoded characters in URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "http://example.com/schemas/%E2%9C%93-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\"}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation exactly matches the percent‑encoded URI string and whose root node equals the supplied JsonNode.",
          "description": "Validates that the method does not decode or otherwise modify percent‑encoded characters in the URI."
        }
      ]
    },
    {
      "group": "Unsupported scheme URI (classpath)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "classpath:/schemas/internal-schema.json"
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation is \"classpath:/schemas/internal-schema.json\" and whose root node equals the supplied JsonNode.",
          "description": "Even though the scheme 'classpath' is not a standard URL scheme, the method treats the URI as a plain string; it should still create a schema instance without error."
        }
      ]
    },
    {
      "group": "Empty string URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": ""
            },
            {
              "parameter": "jsonNode",
              "value": "{\"type\":\"object\"}"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose SchemaLocation is an empty string and whose root node equals the supplied JsonNode.",
          "description": "An empty string is a syntactically valid (relative) URI; the method should accept it and propagate it unchanged to SchemaLocation."
        }
      ]
    }
  ]
}