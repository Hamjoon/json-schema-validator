{
  "testCases": [
    {
      "group": "Standard Cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "/properties/name"
            },
            {
              "parameter": "schemaNode",
              "value": "{\"type\":\"string\"}"
            }
          ],
          "expected": "\"/properties/name\" : {\"type\":\"string\"}",
          "description": "Typical case with a non‑empty schema path and a simple object node. The toString() method should wrap the path in double quotes, add a colon and a space, then append the node's JSON string."
        },
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "/items"
            },
            {
              "parameter": "schemaNode",
              "value": "[{\"type\":\"string\"},{\"type\":\"number\"}]"
            }
          ],
          "expected": "\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]",
          "description": "Complex node (JSON array) – verifies that the node's own toString() representation is used unchanged."
        }
      ]
    },
    {
      "group": "Edge Cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": ""
            },
            {
              "parameter": "schemaNode",
              "value": "{\"type\":\"integer\"}"
            }
          ],
          "expected": "\"\" : {\"type\":\"integer\"}",
          "description": "Empty schema path – the method should still produce a pair of double quotes followed by the colon and node string."
        },
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "prop\"Name"
            },
            {
              "parameter": "schemaNode",
              "value": "{\"type\":\"boolean\"}"
            }
          ],
          "expected": "\"prop\"Name\" : {\"type\":\"boolean\"}",
          "description": "Schema path containing an internal double‑quote character. The method does not escape inner quotes, so they appear verbatim in the result."
        },
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": null
            },
            {
              "parameter": "schemaNode",
              "value": "{\"type\":\"string\"}"
            }
          ],
          "expected": "null\" : {\"type\":\"string\"}",
          "description": "Null schema path – Java concatenates the literal \"null\" string. The output should start with the word null followed by a closing quote, colon and the node."
        }
      ]
    },
    {
      "group": "Exception Cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaPath",
              "value": "/nullNode"
            },
            {
              "parameter": "schemaNode",
              "value": null
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "Null schema node – invoking toString() on a null reference triggers a NullPointerException. The test asserts that the exception is thrown."
        }
      ]
    }
  ]
}