{
  "group": "CollectorContext#get",
  "cases": [
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"missingKey\""
        }
      ],
      "expected": "null",
      "description": "When the requested key is not present in collectorMap, the method should return null (collectorLoadMap is irrelevant because the first lookup yields null)."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"plainObject\""
        }
      ],
      "expected": "\"plainValue\"",
      "description": "Key exists in collectorMap with a value that is NOT a Collector. The condition fails, so the original value from collectorMap is returned."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"collectorOnly\""
        }
      ],
      "expected": "collectorInstance",
      "description": "Key maps to an object that implements Collector, but collectorLoadMap does NOT contain a loaded value for that key (value is null). The method should fall back to returning the Collector instance from collectorMap."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"collectorWithLoad\""
        }
      ],
      "expected": "\"loadedData\"",
      "description": "Key maps to a Collector and collectorLoadMap holds a non‑null loaded value for the same key. The if‑branch is taken and the loaded value is returned, not the Collector itself."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"loadOnly\""
        }
      ],
      "expected": "null",
      "description": "collectorLoadMap contains a value for the key but collectorMap does NOT. Because the first lookup yields null, the instanceof check fails and the method returns null (the value in collectorLoadMap is ignored)."
    }
  ]
}