{
  "testCases": [
    {
      "group": "Standard behavior",
      "cases": [
        {
          "input": [],
          "expected": "\"\" : {\"type\":\"string\"}",
          "description": "When the evaluation path is empty (root) and the schema node is a simple ObjectNode representing {\"type\":\"string\"}, toString() should return the path in quotes followed by a colon and the node's JSON string."
        },
        {
          "input": [],
          "expected": "\"/properties/name\" : {\"type\":\"string\",\"minLength\":1}",
          "description": "When the evaluation path points to a nested property (\"/properties/name\") and the schema node is an ObjectNode with multiple attributes, toString() should correctly embed the full path and the node's JSON representation."
        },
        {
          "input": [],
          "expected": "\"/items[0]\" : [1,2,3]",
          "description": "When the evaluation path contains special characters (array index syntax) and the schema node is an ArrayNode, toString() must still format the output as \"<path>\" : <nodeString>."
        }
      ]
    },
    {
      "group": "Edge cases",
      "cases": [
        {
          "input": [],
          "expected": "\"null\" : {\"type\":\"null\"}",
          "description": "If getEvaluationPath() unexpectedly returns null, the method concatenates the string \"null\" as the path. The test verifies that the method does not throw and returns the literal \"null\" for the path."
        },
        {
          "input": [],
          "expected": "java.lang.NullPointerException",
          "description": "If getSchemaNode() returns null, invoking toString() on the null reference should raise a NullPointerException. This case confirms the method's failure mode when the schema node is missing."
        }
      ]
    }
  ]
}