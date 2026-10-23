{
  "testCases": [
    {
      "group": "Default behavior – enabled unevaluated items",
      "cases": [
        {
          "input": [],
          "expected": {
            "notNull": true,
            "initialSize": 0,
            "addResult": true,
            "sizeAfterAdd": 1,
            "containsAdded": true
          },
          "description": "CollectorContext uses the default dynamic scope where unevaluated items are enabled. getEvaluatedItems() should return a mutable, non‑null collection. Adding an element must succeed, increasing the size to 1 and the element must be present."
        }
      ]
    },
    {
      "group": "Disabled unevaluated items – collection is inert",
      "cases": [
        {
          "input": [],
          "expected": {
            "notNull": true,
            "initialSize": 0,
            "addResult": false,
            "sizeAfterAdd": 0,
            "containsAdded": false
          },
          "description": "Create a CollectorContext whose dynamic scope is constructed with disableUnevaluatedItems = true. getEvaluatedItems() must return the special AbstractCollection that rejects additions. The collection must be non‑null, have size 0, add() must return false, size must stay 0 and the element must not be present."
        }
      ]
    },
    {
      "group": "Null‑safety guarantee",
      "cases": [
        {
          "input": [],
          "expected": {
            "notNull": true
          },
          "description": "Regardless of how the scope is created, getEvaluatedItems() must never return null. This test simply asserts the returned reference is not null."
        }
      ]
    },
    {
      "group": "Effect of merging scopes on the returned collection",
      "cases": [
        {
          "input": [],
          "expected": {
            "mergedSize": 3,
            "containsAll": ["a","b","c"]
          },
          "description": "Create two Scope instances with enabled unevaluated items. Populate the first with \"a\" and \"b\", the second with \"c\". Merge the second into the first via Scope.mergeWith(). Override CollectorContext#getDynamicScope() to return the merged scope. getEvaluatedItems() must reflect all three entries."
        }
      ]
    }
  ]
}