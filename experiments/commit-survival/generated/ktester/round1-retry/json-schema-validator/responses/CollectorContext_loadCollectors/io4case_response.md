{
  "testCases": [
    {
      "group": "loadCollectors - single collector",
      "cases": [
        {
          "input": [],
          "expected": "collectorLoadMap should contain a single entry with key 'c1' and value 'collected1' after invoking loadCollectors() when collectorMap has one Collector entry",
          "description": "Verifies that a Collector instance is invoked and its result stored under the same key in collectorLoadMap."
        }
      ]
    },
    {
      "group": "loadCollectors - multiple collectors",
      "cases": [
        {
          "input": [],
          "expected": "collectorLoadMap should contain three entries with keys 'c1','c2','c3' and values 'v1','v2','v3' respectively after loadCollectors() when collectorMap holds three Collector entries",
          "description": "Ensures that loadCollectors iterates over all entries and populates collectorLoadMap for each Collector."
        }
      ]
    },
    {
      "group": "loadCollectors - non‑collector entries ignored",
      "cases": [
        {
          "input": [],
          "expected": "collectorLoadMap should remain empty after loadCollectors() when collectorMap contains only non‑Collector objects (e.g., String, Integer)",
          "description": "Confirms that entries whose values are not instances of Collector are skipped."
        }
      ]
    },
    {
      "group": "loadCollectors - mixed collectors and non‑collectors",
      "cases": [
        {
          "input": [],
          "expected": "collectorLoadMap should contain only the entries for keys 'c1' and 'c3' with values 'val1' and 'val3' after loadCollectors() when collectorMap has a mix of Collector and non‑Collector values",
          "description": "Tests that loadCollectors correctly processes Collector values while ignoring others in a mixed map."
        }
      ]
    },
    {
      "group": "loadCollectors - collector returns null",
      "cases": [
        {
          "input": [],
          "expected": "collectorLoadMap should contain key 'nullCollector' with a null value after loadCollectors() when the Collector's collect() method returns null",
          "description": "Verifies that a null result from collect() is stored as a null value in collectorLoadMap."
        }
      ]
    },
    {
      "group": "loadCollectors - collector throws exception",
      "cases": [
        {
          "input": [],
          "expected": "loadCollectors() should propagate the RuntimeException thrown by the Collector's collect() method",
          "description": "Ensures that loadCollectors does not swallow exceptions from a failing Collector."
        }
      ]
    },
    {
      "group": "loadCollectors - overwrite existing collectorLoadMap entries",
      "cases": [
        {
          "input": [],
          "expected": "collectorLoadMap should contain the new values 'new1' for key 'c1' after a second call to loadCollectors() when collectorMap still has a Collector for 'c1' but its collect() now returns a different value",
          "description": "Checks that subsequent invocations replace previous entries rather than preserving stale data."
        }
      ]
    }
  ]
}