{
  "group": "BaseJsonValidator.atRoot() - PathType handling",
  "cases": [
    {
      "input": [],
      "expected": "JsonNodePathJsonPointer singleton instance",
      "description": "If the ValidationContext's Config returns PathType.JSON_POINTER, the method should return JsonNodePathJsonPointer.getInstance()."
    },
    {
      "input": [],
      "expected": "JsonNodePathLegacy singleton instance",
      "description": "If the ValidationContext's Config returns PathType.LEGACY, the method should return JsonNodePathLegacy.getInstance()."
    },
    {
      "input": [],
      "expected": "JsonNodePathJsonPath singleton instance",
      "description": "If the ValidationContext's Config returns PathType.JSON_PATH, the method should return JsonNodePathJsonPath.getInstance()."
    },
    {
      "input": [],
      "expected": "new JsonNodePath instance with the custom PathType",
      "description": "When the Config returns a PathType that is not JSON_POINTER, LEGACY, or JSON_PATH (e.g., PathType.CUSTOM), the method should fall back to creating a new JsonNodePath using that PathType."
    }
  ]
}