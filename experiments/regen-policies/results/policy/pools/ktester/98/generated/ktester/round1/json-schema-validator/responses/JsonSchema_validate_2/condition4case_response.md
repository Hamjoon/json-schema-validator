{
  "testCases": [
    {
      "group": "Valid JSON satisfies schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"name\":\"John\"}"
            }
          ],
          "expected": "empty set []",
          "description": "The schema requires a string property \"name\". Providing a JsonNode with a valid string value should result in an empty ValidationMessage set (no errors)."
        }
      ]
    },
    {
      "group": "Missing required property triggers validation error",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{}"
            }
          ],
          "expected": "non‑empty set containing a message about missing required property 'name'",
          "description": "The schema declares \"name\" as required. An empty JsonNode should cause validate(JsonNode) to return a set with at least one ValidationMessage indicating the missing required property."
        }
      ]
    },
    {
      "group": "Wrong type for property triggers validation error",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"name\":123}"
            }
          ],
          "expected": "non‑empty set containing a message about type mismatch for property 'name'",
          "description": "The schema expects \"name\" to be a string. Supplying a numeric value should cause a type‑validation error to be reported."
        }
      ]
    },
    {
      "group": "Null JsonNode handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling validate(null) should throw a NullPointerException because the implementation forwards the argument to the overloaded method which expects a non‑null JsonNode."
        }
      ]
    },
    {
      "group": "Schema with no constraints accepts any JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{\"anyKey\":\"anyValue\", \"num\":42}"
            }
          ],
          "expected": "empty set []",
          "description": "When the JsonSchema is created from an empty schema (\"{}\"), any JsonNode is valid. The method should return an empty ValidationMessage set regardless of the node's content."
        }
      ]
    }
  ]
}