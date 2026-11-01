{
  "groups": [
    {
      "group": "getNode(Object) – property name URL‑encoded with plus sign",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"first+name\""
            }
          ],
          "expected": "JsonNode with text value \"John\"",
          "description": "When the key is encoded with a '+' (which URLDecoder translates to a space), the method should decode it to \"first name\" and return the matching JsonNode."
        }
      ]
    },
    {
      "group": "getNode(Object) – property name URL‑encoded malformed sequence",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"bad%2G\""
            }
          ],
          "expected": "null",
          "description": "A malformed percent‑encoding should be ignored (or cause a safe fallback) and the lookup should return null instead of throwing an exception."
        }
      ]
    },
    {
      "group": "getNode(Object) – array index (Double) coerced to int",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "1.9"
            }
          ],
          "expected": "JsonNode with numeric value 20",
          "description": "A Double index is converted via Number.intValue() (truncating the decimal) and the element at index 1 is returned."
        }
      ]
    },
    {
      "group": "getNode(Object) – array index negative",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "-1"
            }
          ],
          "expected": "null",
          "description": "Negative indexes are out of bounds; the method should return null rather than throwing an exception."
        }
      ]
    },
    {
      "group": "getNode(Object) – Number used on ObjectNode",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "2"
            }
          ],
          "expected": "null",
          "description": "When the underlying schemaNode is an ObjectNode, a numeric argument is treated as a String key (\"2\"). If such a field does not exist, null is returned."
        }
      ]
    },
    {
      "group": "getNode(Object) – empty string key",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"\""
            }
          ],
          "expected": "JsonNode with text value \"empty\"",
          "description": "ObjectNode may contain a field with an empty name; the method should correctly retrieve it when the empty string is supplied."
        }
      ]
    },
    {
      "group": "getNode(Object) – Unicode key URL‑decoded",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "\"%E6%BC%A2%E5%AD%97\""
            }
          ],
          "expected": "JsonNode with text value \"漢字\"",
          "description": "The URL‑encoded Unicode sequence should be decoded to the Chinese characters \"漢字\" and the corresponding field should be returned."
        }
      ]
    },
    {
      "group": "getNode(Object) – propertyOrIndex as Character (unsupported type)",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "'c'"
            }
          ],
          "expected": "null",
          "description": "A Character is not a String or Number; the method should fall back to returning null."
        }
      ]
    }
  ]
}