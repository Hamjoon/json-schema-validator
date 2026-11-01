{
  "group": "JSON Pointer branch – getSubSchema is used",
  "cases": [
    {
      "input": [
        {
          "parameter": "fragment",
          "value": "JsonNodePath mock where getPathType() returns PathType.JSON_POINTER"
        }
      ],
      "expected": "The JsonSchema instance returned by getSubSchema(fragment) is returned unchanged.",
      "description": "When the fragment path type is JSON_POINTER the method should delegate to getSubSchema(fragment) and return its result. This test verifies that the delegation occurs and no other logic is executed."
    }
  ]
}