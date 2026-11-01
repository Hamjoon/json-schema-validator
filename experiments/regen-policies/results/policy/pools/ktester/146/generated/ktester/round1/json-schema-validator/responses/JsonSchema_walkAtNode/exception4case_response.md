{
  "testCases": [
    {
      "group": "Null instanceLocation – defensive handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"name\": \"John\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "NullPointerException",
          "description": "The method should validate that the instanceLocation argument is not null and throw a NullPointerException when it is null."
        }
      ]
    },
    {
      "group": "Unresolvable $ref – IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"refNode\": {} }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"refNode\": {} }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "The schema contains a $ref that points to a non‑existent definition (e.g., \"#/components/schemas/Unknown\"). walkAtNode should detect the unresolved reference and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Malformed $ref URI – UnsupportedEncodingException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"badRef\": {} }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"badRef\": {} }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "UnsupportedEncodingException",
          "description": "The schema uses a $ref with an illegal URI component (e.g., \"#/components/schemas/%ZZ\"). During resolution the code calls URLDecoder, which throws UnsupportedEncodingException. The test verifies that this exception propagates."
        }
      ]
    },
    {
      "group": "External schema loading failure – IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"external\": {} }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"external\": {} }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.root()"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IOException",
          "description": "The schema contains a $ref that points to an external file (e.g., \"file:///nonexistent/schema.json\"). When walkAtNode attempts to load the external resource it fails, resulting in an IOException."
        }
      ]
    },
    {
      "group": "Invalid JsonNodePath segment – IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "{ \"name\": \"John\" }"
            },
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"John\" }"
            },
            {
              "parameter": "instanceLocation",
              "value": "JsonNodePath.of(null)"
            },
            {
              "parameter": "validate",
              "value": "true"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "JsonNodePath.of(null) creates a path containing a null segment. walkAtNode should detect the illegal path element and throw an IllegalArgumentException."
        }
      ]
    }
  ]
}