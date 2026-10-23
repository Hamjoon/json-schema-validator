{
  "testCases": [
    {
      "group": "CollectorContext.getAll() - null key handling",
      "cases": [
        {
          "input": [],
          "expected": "Map contains a null key with value \"collectorNull\" and no other entries.",
          "description": "collectorMap has a single entry with a null key and value \"collectorNull\"; collectorLoadMap is empty. The returned map should preserve the null key and its value."
        },
        {
          "input": [],
          "expected": "Map contains a null key with value \"loadNull\" and no other entries.",
          "description": "collectorLoadMap has a single entry with a null key and value \"loadNull\"; collectorMap is empty. The returned map should preserve the null key and its value."
        },
        {
          "input": [],
          "expected": "Map contains a null key with value \"loadOverride\" (value from collectorLoadMap) and no other entries.",
          "description": "Both collectorMap and collectorLoadMap contain a null key; collectorMap maps null → \"collectorOverride\", collectorLoadMap maps null → \"loadOverride\". The value from collectorLoadMap should win in the merged result."
        }
      ]
    },
    {
      "group": "CollectorContext.getAll() - null value handling",
      "cases": [
        {
          "input": [],
          "expected": "Map contains key \"k1\" with value null and key \"k2\" with value \"val2\".",
          "description": "collectorMap has entry k1→null, collectorLoadMap has entry k2→\"val2\". Null values should be kept unchanged in the merged map."
        },
        {
          "input": [],
          "expected": "Map contains key \"k1\" with value \"loadVal\" and key \"k2\" with value null (value from collectorLoadMap).",
          "description": "collectorMap has k1→\"collectorVal\", collectorLoadMap has k1→null and k2→null. For duplicate key k1, the null from collectorLoadMap overrides; k2 appears with null."
        }
      ]
    },
    {
      "group": "CollectorContext.getAll() - map instance isolation",
      "cases": [
        {
          "input": [],
          "expected": "Two successive calls to getAll() return distinct Map instances (different object references).",
          "description": "Invoke getAll() twice without modifying internal maps and assert that the returned Map objects are not the same instance."
        },
        {
          "input": [],
          "expected": "A map returned earlier does not reflect later changes to internal collectorMap or collectorLoadMap.",
          "description": "Call getAll() to obtain map1, then add a new entry to collectorMap, call getAll() again to obtain map2. Verify that map1 does not contain the newly added entry while map2 does."
        }
      ]
    },
    {
      "group": "CollectorContext.getAll() - large merge stress test",
      "cases": [
        {
          "input": [],
          "expected": "Returned map contains 200 distinct entries (keys \"c0\"‑\"c99\" from collectorMap and \"l0\"‑\"l99\" from collectorLoadMap) with correct corresponding integer values.",
          "description": "Populate collectorMap with 100 entries (c0→0 … c99→99) and collectorLoadMap with another 100 distinct entries (l0→100 … l99→199). The merged map should contain all 200 entries."
        }
      ]
    }
  ]
}