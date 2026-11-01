{
  "testCases": [
    {
      "group": "Normal behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance whose getSchemaLocation().toString() equals \"http://example.com/schema.json\" and whose getSchemaNode() equals the supplied JsonNode",
          "description": "Verify that a valid URI, a valid JsonNode and a non‑null config produce a JsonSchema whose location and underlying node match the inputs."
        }
      ]
    },
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "null"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling getSchema with a null URI should throw NPE because schemaUri.toString() is invoked."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json\")"
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
          "expected": "NullPointerException",
          "description": "Passing a null JsonNode should result in NPE when newJsonSchema attempts to process the node."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"number\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonSchema instance with default SchemaValidatorsConfig (i.e., config treated as null and replaced internally)",
          "description": "When config is null the factory should fall back to a default configuration rather than throwing."
        }
      ]
    },
    {
      "group": "Edge cases for URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"array\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance whose location string is empty (\"\") and node matches the supplied JsonNode",
          "description": "An empty URI string is allowed; the resulting SchemaLocation should be empty and no exception should be thrown."
        },
        {
          "input": [
            {
              "parameter": "schemaUri",
              "value": "new URI(\"file:/nonexistent/path/schema.json\")"
            },
            {
              "parameter": "jsonNode",
              "value": "ObjectMapper().readTree(\"{\\\"type\\\":\\\"boolean\\\"}\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonSchema instance with location \"file:/nonexistent/path/schema.json\"; no I/O is performed because the node is already supplied",
          "description": "When a URI points to a non‑existent resource but a JsonNode is provided, the factory should ignore the URI's existence and simply use the node."
        }
      ]
    }
  ]
}