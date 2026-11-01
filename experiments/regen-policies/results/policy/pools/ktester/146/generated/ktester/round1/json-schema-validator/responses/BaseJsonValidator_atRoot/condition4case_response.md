{
  "group": "BaseJsonValidator.atRoot() branch coverage",
  "cases": [
    {
      "input": [],
      "expected": "instance of JsonNodePathJsonPointer (singleton)",
      "description": "When the ValidationContext's Config returns PathType.JSON_POINTER, atRoot() should return the singleton JsonNodePathJsonPointer instance."
    },
    {
      "input": [],
      "expected": "instance of JsonNodePathLegacy (singleton)",
      "description": "When the Config returns PathType.LEGACY, atRoot() should return the singleton JsonNodePathLegacy instance."
    },
    {
      "input": [],
      "expected": "instance of JsonNodePathJsonPath (singleton)",
      "description": "When the Config returns PathType.JSON_PATH, atRoot() should return the singleton JsonNodePathJsonPath instance."
    },
    {
      "input": [],
      "expected": "new JsonNodePath with the provided custom PathType",
      "description": "When the Config returns a PathType that is not JSON_POINTER, LEGACY, or JSON_PATH (e.g., a custom enum value), atRoot() should fall back to creating a new JsonNodePath instance initialized with that custom PathType."
    }
  ]
}