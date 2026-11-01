{
  "testCases": [
    {
      "group": "Null reference handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null reference should cause a NullPointerException because the method invokes ref.startsWith(...) without a null‑check."
        }
      ]
    },
    {
      "group": "Empty reference handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": ""
            }
          ],
          "expected": "Root schema JsonNode (no processing performed)",
          "description": "An empty string does not trigger ID stripping, '#/' removal, or '/' handling, so the method should return the original root schema node unchanged."
        }
      ]
    },
    {
      "group": "Exact ID match (no pointer)",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "http://example.com/schema"
            }
          ],
          "expected": "Root schema JsonNode (ID stripped leaving empty pointer)",
          "description": "When the reference exactly equals the schema ID, the ID part is stripped, leaving an empty jsonPointer. Because it does not start with '/', the method returns the root schema node."
        }
      ]
    },
    {
      "group": "Reference without leading slash after ID stripping",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "http://example.com/schemaproperties/name"
            }
          ],
          "expected": "Root schema JsonNode (pointer does not start with '/' after ID stripping)",
          "description": "The reference starts with the schema ID, which is stripped, leaving 'properties/name' (no leading '/'). The method therefore skips pointer resolution and returns the root node."
        }
      ]
    },
    {
      "group": "Reference with plus sign decoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties+name"
            }
          ],
          "expected": "JsonNode located at '/properties name' of the root schema (plus decoded to space)",
          "description": "The '+' character is decoded by URLDecoder into a space character. After decoding, the pointer becomes '/properties name' which is resolved against the root schema."
        }
      ]
    },
    {
      "group": "Reference with encoded space (%20)",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties%20name"
            }
          ],
          "expected": "JsonNode located at '/properties name' of the root schema (encoded space decoded)",
          "description": "The reference contains an encoded space (%20). The method decodes it to a literal space before resolving the pointer."
        }
      ]
    },
    {
      "group": "Fragment only reference without leading slash",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "#definitions/person"
            }
          ],
          "expected": "Root schema JsonNode (fragment not prefixed with '#/', so no stripping; pointer does not start with '/', method returns root node)",
          "description": "A fragment that starts with '#', but not with '#/', bypasses the '#/' removal logic. Since the resulting jsonPointer does not begin with '/', the method returns the root schema node unchanged."
        }
      ]
    },
    {
      "group": "Encoded slash after ID stripping leading to missing node",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "http://example.com/schema#/nonexistent%2Fpath"
            }
          ],
          "expected": "Result of handleNullNode(ref, schema) (decoded pointer '/nonexistent/path' resolves to MissingNode, triggering handleNullNode)",
          "description": "ID is stripped, leaving '#/nonexistent%2Fpath'. After '#/' removal, jsonPointer becomes '/nonexistent%2Fpath', which is decoded to '/nonexistent/path'. The node is missing, so handleNullNode should be invoked."
        }
      ]
    }
  ]
}