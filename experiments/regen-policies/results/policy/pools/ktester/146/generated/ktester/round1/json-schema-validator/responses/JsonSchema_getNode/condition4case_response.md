{
  "testCases": [
    {
      "group": "getNode(Object) – property name (String) exists",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"name\""
            }
          ],
          "expected": "JsonNode with text value \"John\"",
          "description": "When the schemaNode is an ObjectNode containing a field \"name\":\"John\" and the argument is the exact String key, getNode should return the corresponding JsonNode."
        }
      ]
    },
    {
      "group": "getNode(Object) – property name (String) does NOT exist",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"missing\""
            }
          ],
          "expected": "null",
          "description": "If the ObjectNode does not contain the requested field, the method should return null."
        }
      ]
    },
    {
      "group": "getNode(Object) – property name (String) URL‑encoded",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"first%20name\""
            }
          ],
          "expected": "JsonNode with text value \"John\"",
          "description": "When the key is URL‑encoded, the method should decode it (using URLDecoder) before looking up the field."
        }
      ]
    },
    {
      "group": "getNode(Object) – array index (Integer) within bounds",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "1"
            }
          ],
          "expected": "JsonNode with numeric value 20",
          "description": "If the schemaNode is an ArrayNode [10,20,30] and the argument is an Integer index that exists, the method should return the element at that index."
        }
      ]
    },
    {
      "group": "getNode(Object) – array index (Integer) out of bounds",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "5"
            }
          ],
          "expected": "null",
          "description": "When the index is greater than the last element of the array, the method should return null rather than throwing an exception."
        }
      ]
    },
    {
      "group": "getNode(Object) – array index (Long) coerced to int",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "2L"
            }
          ],
          "expected": "JsonNode with numeric value 30",
          "description": "If the argument is a Number type other than Integer (e.g., Long), the method should convert it to int and retrieve the element."
        }
      ]
    },
    {
      "group": "getNode(Object) – propertyOrIndex is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "A null argument should cause the method to return null immediately."
        }
      ]
    },
    {
      "group": "getNode(Object) – propertyOrIndex of unsupported type (e.g., Boolean)",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "true"
            }
          ],
          "expected": "null",
          "description": "When the argument is neither a String nor a Number, the method should fall back to returning null."
        }
      ]
    },
    {
      "group": "getNode(Object) – node is not an ObjectNode/ArrayNode (e.g., text node)",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"any\""
            }
          ],
          "expected": "null",
          "description": "If the underlying schemaNode is a primitive JsonNode (e.g., text), any property lookup should result in null."
        }
      ]
    },
    {
      "group": "getNode(Object) – property name with special characters after decoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"spaced%20key\""
            }
          ],
          "expected": "JsonNode with text value \"value\"",
          "description": "Ensures that URL decoding correctly handles '+' and '%' sequences before the lookup."
        }
      ]
    }
  ]
}