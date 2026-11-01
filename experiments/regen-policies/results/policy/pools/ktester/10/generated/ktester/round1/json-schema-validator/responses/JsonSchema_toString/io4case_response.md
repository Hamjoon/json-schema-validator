{
  "testCases": [
    {
      "group": "JsonSchema.toString - both null",
      "cases": [
        {
          "input": [],
          "expected": "java.lang.NullPointerException",
          "description": "When both getEvaluationPath() and getSchemaNode() return null, the method attempts to invoke toString() on the null schema node, which results in a NullPointerException."
        }
      ]
    },
    {
      "group": "JsonSchema.toString - empty evaluation path",
      "cases": [
        {
          "input": [],
          "expected": "\"\" : {\"type\":\"boolean\"}",
          "description": "When getEvaluationPath() returns an empty string and getSchemaNode() returns a JsonNode representing {\"type\":\"boolean\"}, toString() should produce a string with empty quotes before the colon."
        }
      ]
    },
    {
      "group": "JsonSchema.toString - whitespace evaluation path",
      "cases": [
        {
          "input": [],
          "expected": "\"  \" : {\"type\":\"string\"}",
          "description": "When getEvaluationPath() returns a string consisting solely of whitespace characters, those spaces must be preserved inside the quotes in the resulting string."
        }
      ]
    },
    {
      "group": "JsonSchema.toString - special characters in path",
      "cases": [
        {
          "input": [],
          "expected": "\"/properties/name with space\" : {\"type\":\"string\"}",
          "description": "When getEvaluationPath() contains spaces (or other printable characters), they should appear unchanged in the output, demonstrating that the method does not trim or alter the path."
        }
      ]
    },
    {
      "group": "JsonSchema.toString - complex schema node",
      "cases": [
        {
          "input": [],
          "expected": "\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]",
          "description": "When getSchemaNode() is an array node containing multiple element schemas, its toString() representation (a JSON array) must be concatenated correctly with the evaluation path."
        }
      ]
    },
    {
      "group": "JsonSchema.toString - repeated calls produce equal strings",
      "cases": [
        {
          "input": [],
          "expected": "identical strings",
          "description": "Calling toString() multiple times on the same JsonSchema instance should return equal string values each time (the content must be identical, even if the object reference differs)."
        }
      ]
    }
  ]
}