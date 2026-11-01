{
  "newTestCases": [
    {
      "group": "Object node – numeric index",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"a\":1,\"b\":2}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 0
            }
          ],
          "expected": null,
          "description": "When the node is an ObjectNode and the propertyOrIndex is an Integer, getNode should return null because numeric indexes are not applicable to objects."
        }
      ]
    },
    {
      "group": "Array node – string index",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[\"x\",\"y\"]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "1"
            }
          ],
          "expected": null,
          "description": "When the node is an ArrayNode and the propertyOrIndex is a String, getNode should return null because string keys are not valid indexes for arrays."
        }
      ]
    },
    {
      "group": "Scalar node handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"plain text\""
            },
            {
              "parameter": "propertyOrIndex",
              "value": "any"
            }
          ],
          "expected": null,
          "description": "If the supplied JsonNode is a scalar (text) value, any propertyOrIndex should result in null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "42"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 0
            }
          ],
          "expected": null,
          "description": "If the supplied JsonNode is a numeric scalar, getNode should return null regardless of the propertyOrIndex."
        }
      ]
    },
    {
      "group": "Null propertyOrIndex",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"k\":\"v\"}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": null
            }
          ],
          "expected": null,
          "description": "A null propertyOrIndex should be treated as absent and cause getNode to return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "[\"a\"]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": null
            }
          ],
          "expected": null,
          "description": "When propertyOrIndex is null and the node is an array, getNode should return null."
        }
      ]
    },
    {
      "group": "Empty string property",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"\":\"empty\"}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": ""
            }
          ],
          "expected": "\"empty\"",
          "description": "An empty string is a valid property name; getNode should retrieve the associated value."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": ""
            }
          ],
          "expected": null,
          "description": "If the empty string property does not exist in the object, getNode should return null."
        }
      ]
    },
    {
      "group": "Large index out of range",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[0,1]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 2147483647
            }
          ],
          "expected": null,
          "description": "An index far beyond the array size should be considered out‑of‑bounds and return null."
        }
      ]
    },
    {
      "group": "Unsupported propertyOrIndex type – Character",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"c\":\"value\"}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "c"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing a Character (represented here as a single‑character String) is not a supported type for propertyOrIndex; JsonNodes.get should throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported propertyOrIndex type – Byte",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"b\":10}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 5
            }
          ],
          "expected": null,
          "description": "Even though the numeric value fits in a byte, JsonNodes.get treats it as an Integer index; when the node is an object, the result should be null (not an exception)."
        }
      ]
    }
  ]
}