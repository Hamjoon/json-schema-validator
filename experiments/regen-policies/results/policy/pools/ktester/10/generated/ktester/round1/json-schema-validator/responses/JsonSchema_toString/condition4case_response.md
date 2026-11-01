{
  "testCases": [
    {
      "group": "JsonSchema.toString - normal operation",
      "cases": [
        {
          "input": [],
          "expected": "\"/properties/name\" : {\"type\":\"string\"}",
          "description": "When getEvaluationPath() returns a non‑null path and getSchemaNode() returns a non‑null JsonNode, toString() should concatenate them in the format \"<path>\" : <node>. A spy of JsonSchema is used to stub getEvaluationPath() to return \"/properties/name\" and getSchemaNode() to return a JsonNode whose toString() yields {\"type\":\"string\"}."
        }
      ]
    },
    {
      "group": "JsonSchema.toString - null evaluation path",
      "cases": [
        {
          "input": [],
          "expected": "\"null\" : {\"type\":\"integer\"}",
          "description": "If getEvaluationPath() returns null, Java string concatenation converts it to the literal \"null\". The method should still produce a valid string without throwing. A spy is configured to return null for getEvaluationPath() and a normal JsonNode for getSchemaNode()."
        }
      ]
    },
    {
      "group": "JsonSchema.toString - null schema node",
      "cases": [
        {
          "input": [],
          "expected": "java.lang.NullPointerException",
          "description": "When getSchemaNode() returns null, the call to getSchemaNode().toString() triggers a NullPointerException. The test verifies that the exception is thrown. A spy is configured to return a non‑null path and null for getSchemaNode()."
        }
      ]
    }
  ]
}