{
  "testCases": [
    {
      "group": "BaseJsonValidator.atRoot() - Singleton identity",
      "cases": [
        {
          "input": [],
          "expected": "Same instance as JsonNodePathJsonPointer.getInstance() (object identity)",
          "description": "When the ValidationContext's Config returns PathType.JSON_POINTER, atRoot() should return the singleton instance provided by JsonNodePathJsonPointer.getInstance(), and the returned object must be identical (==) to that singleton."
        },
        {
          "input": [],
          "expected": "Same instance as JsonNodePathLegacy.getInstance() (object identity)",
          "description": "When the ValidationContext's Config returns PathType.LEGACY, atRoot() should return the singleton instance provided by JsonNodePathLegacy.getInstance(), and the returned object must be identical (==) to that singleton."
        },
        {
          "input": [],
          "expected": "Same instance as JsonNodePathJsonPath.getInstance() (object identity)",
          "description": "When the ValidationContext's Config returns PathType.JSON_PATH, atRoot() should return the singleton instance provided by JsonNodePathJsonPath.getInstance(), and the returned object must be identical (==) to that singleton."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.atRoot() - Custom PathType instance behavior",
      "cases": [
        {
          "input": [],
          "expected": "Two successive calls return different JsonNodePath objects (not the same instance)",
          "description": "For a non‑standard PathType (e.g., PathType.CUSTOM), atRoot() creates a new JsonNodePath each time. Invoking atRoot() twice on the same validator should yield two distinct objects (instance equality should be false)."
        },
        {
          "input": [],
          "expected": "Returned JsonNodePath.getPathType() equals the custom PathType supplied by the Config",
          "description": "When the Config returns a custom PathType, the JsonNodePath created by atRoot() must carry that exact PathType (accessible via its getPathType() method)."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.atRoot() - Null safety",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "If the validator's validationContext field is null, calling atRoot() should throw a NullPointerException because the method dereferences validationContext."
        },
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "If validationContext.getConfig() returns null, atRoot() will attempt to call getPathType() on a null reference and must throw a NullPointerException."
        },
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "If validationContext.getConfig().getPathType() returns null, the equals() checks inside atRoot() will cause a NullPointerException; the method should propagate this exception."
        }
      ]
    }
  ]
}