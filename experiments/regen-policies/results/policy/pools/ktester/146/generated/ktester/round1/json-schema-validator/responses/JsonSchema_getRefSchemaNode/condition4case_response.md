{
  "testCases": [
    {
      "group": "Schema ID handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties/name"
            }
          ],
          "expected": "JsonNode located at '/properties/name' of the root schema (no ID stripping, pointer resolved)",
          "description": "Schema has no ID (findSchemaResourceRoot().getId() returns null). The reference starts with '/' so it is decoded (no encoded chars) and resolved directly against the root schema node."
        },
        {
          "input": [
            {
              "parameter": "ref",
              "value": "http://example.com/schema/properties/age"
            }
          ],
          "expected": "JsonNode located at '/properties/age' of the root schema (ID stripped, pointer resolved)",
          "description": "Schema ID is 'http://example.com/schema'. The reference starts with this ID, therefore the ID part is stripped leaving '/properties/age'. The pointer is then resolved against the root schema node."
        }
      ]
    },
    {
      "group": "Fragment handling with '#/' prefix",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "#/definitions/person"
            }
          ],
          "expected": "JsonNode located at '/definitions/person' of the root schema (fragment stripped, pointer resolved)",
          "description": "Reference begins with '#/'. After optional ID stripping, the leading '#/' is removed (substring(1)) resulting in '/definitions/person', which is then resolved."
        },
        {
          "input": [
            {
              "parameter": "ref",
              "value": "http://example.com/schema#/definitions/address"
            }
          ],
          "expected": "JsonNode located at '/definitions/address' of the root schema (ID stripped, fragment stripped, pointer resolved)",
          "description": "Reference contains both an ID and a fragment. ID is stripped first, leaving '#/definitions/address'. The '#/' prefix is then removed, yielding '/definitions/address' which is resolved."
        }
      ]
    },
    {
      "group": "No leading '/' after processing",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "definitions/simple"
            }
          ],
          "expected": "Root schema JsonNode (no pointer processing because it does not start with '/' after previous steps)",
          "description": "After ID and '#/' handling, the resulting jsonPointer does not start with '/' so the method returns the original root schema node unchanged."
        }
      ]
    },
    {
      "group": "Missing node handling (handleNullNode invoked)",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/nonexistent/path"
            }
          ],
          "expected": "Result of handleNullNode(ref, schema) (the method is called because node.at('/nonexistent/path') returns MissingNode)",
          "description": "The pointer resolves to a missing node; the method should call handleNullNode and return its result."
        }
      ]
    },
    {
      "group": "URL decoding of encoded characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties%2Fname"
            }
          ],
          "expected": "JsonNode located at '/properties/name' of the root schema (encoded '/' decoded before resolution)",
          "description": "Reference contains an encoded '/' (%2F). After ID and fragment handling, the pointer starts with '/', the method decodes it using UTF‑8, turning it into '/properties/name' before resolution."
        }
      ]
    }
  ]
}