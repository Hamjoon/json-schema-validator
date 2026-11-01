{
  "testCases": [
    {
      "group": "No ID, simple absolute JSON Pointer",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties/name"
            }
          ],
          "expected": "JsonNode representing the schema node located at '/properties/name' (e.g., {\"type\":\"string\"})",
          "description": "The schema has no ID, the reference starts with '/' so it is treated as an absolute JSON Pointer. The method should return the node found at that pointer."
        }
      ]
    },
    {
      "group": "Schema ID present and reference starts with the ID",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "http://example.com/schema#/properties/age"
            }
          ],
          "expected": "JsonNode representing the schema node located at '/properties/age' (e.g., {\"type\":\"integer\"})",
          "description": "The schema's getId() returns 'http://example.com/schema'. Because the reference starts with this ID, the ID part is stripped, leaving '#/properties/age'. The leading '#/' is then removed, yielding '/properties/age', which is used to locate the node."
        }
      ]
    },
    {
      "group": "Reference starts with '#/' fragment only",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "#/definitions/person"
            }
          ],
          "expected": "JsonNode representing the schema node located at '/definitions/person' (e.g., {\"type\":\"object\", ...})",
          "description": "No schema ID is involved. The reference begins with '#/', so the method removes the leading '#', leaving '/definitions/person', and returns the node at that pointer."
        }
      ]
    },
    {
      "group": "URL‑decoded pointer",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties/first%20name"
            }
          ],
          "expected": "JsonNode representing the schema node located at '/properties/first name' (decoded space) (e.g., {\"type\":\"string\"})",
          "description": "The pointer contains percent‑encoded characters. After confirming it starts with '/', the method URL‑decodes it to '/properties/first name' before locating the node."
        }
      ]
    },
    {
      "group": "Missing node – fallback to handleNullNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/nonexistent/path"
            }
          ],
          "expected": "JsonNode returned by handleNullNode(ref, schema) (fallback node)",
          "description": "The pointer points to a location that does not exist in the schema. After node.at() returns a missing node, the method invokes handleNullNode and returns its result."
        }
      ]
    },
    {
      "group": "Reference does not start with '/' or '#/' – returns root schema node",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "plainReference"
            }
          ],
          "expected": "The original root schema JsonNode (schema.getSchemaNode())",
          "description": "The reference neither starts with the schema ID, '#/', nor '/' so none of the conditional blocks modify jsonPointer. The method should simply return the root schema node."
        }
      ]
    }
  ]
}