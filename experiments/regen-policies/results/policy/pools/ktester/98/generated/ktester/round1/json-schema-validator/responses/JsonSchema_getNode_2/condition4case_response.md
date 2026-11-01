{
  "testCases": [
    {
      "group": "Object node – existing property",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "name"
            }
          ],
          "expected": "\"John\"",
          "description": "When the node is an ObjectNode and the property string exists, getNode should return the corresponding child JsonNode containing the value \"John\"."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "age"
            }
          ],
          "expected": "30",
          "description": "When the node is an ObjectNode and the property string exists, getNode should return the numeric child JsonNode (30)."
        }
      ]
    },
    {
      "group": "Object node – non‑existing property",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": "address"
            }
          ],
          "expected": null,
          "description": "If the property does not exist in the ObjectNode, getNode should return null."
        }
      ]
    },
    {
      "group": "Array node – valid index",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[\"apple\",\"banana\",\"cherry\"]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 1
            }
          ],
          "expected": "\"banana\"",
          "description": "When the node is an ArrayNode and the index is within bounds, getNode should return the element at that index."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "[10,20,30]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 0
            }
          ],
          "expected": "10",
          "description": "Array index 0 should return the first numeric element."
        }
      ]
    },
    {
      "group": "Array node – out‑of‑bounds index",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "[\"a\",\"b\"]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 5
            }
          ],
          "expected": null,
          "description": "If the index is greater than the last element, getNode should return null."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "[]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": -1
            }
          ],
          "expected": null,
          "description": "Negative indexes are considered out of bounds and should result in null."
        }
      ]
    },
    {
      "group": "Invalid propertyOrIndex type",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"key\":\"value\"}"
            },
            {
              "parameter": "propertyOrIndex",
              "value": true
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing a Boolean as propertyOrIndex is not supported; JsonNodes.get should throw IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": "[1,2,3]"
            },
            {
              "parameter": "propertyOrIndex",
              "value": 3.14
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passing a non‑integer number as index should cause IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Null node handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "propertyOrIndex",
              "value": "any"
            }
          ],
          "expected": null,
          "description": "If the supplied JsonNode is null, getNode should safely return null rather than throwing NPE."
        },
        {
          "input": [
            {
              "parameter": "node",
              "value": null
            },
            {
              "parameter": "propertyOrIndex",
              "value": 0
            }
          ],
          "expected": null,
          "description": "Null node with numeric index also returns null."
        }
      ]
    }
  ]
}