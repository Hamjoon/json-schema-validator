{
  "testCases": [
    {
      "group": "hasTypeValidator - when a TypeValidator is present",
      "cases": [
        {
          "input": [],
          "expected": true,
          "description": "A JsonSchema instance is spied and its protected getTypeValidator() method is stubbed to return a non‑null mock TypeValidator. The hasTypeValidator() method should return true because a type validator exists."
        }
      ]
    },
    {
      "group": "hasTypeValidator - when no TypeValidator is present",
      "cases": [
        {
          "input": [],
          "expected": false,
          "description": "A JsonSchema instance is spied and its protected getTypeValidator() method is stubbed to return null. The hasTypeValidator() method should return false because there is no type validator."
        }
      ]
    }
  ]
}