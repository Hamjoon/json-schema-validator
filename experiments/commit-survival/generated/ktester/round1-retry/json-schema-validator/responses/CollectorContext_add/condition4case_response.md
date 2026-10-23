{
  "group": "CollectorContext.add(String, Collector<E>) - basic functionality and edge cases",
  "cases": [
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"testCollector\""
        },
        {
          "parameter": "collector",
          "value": "new SimpleCollector<>()"
        }
      ],
      "expected": "CollectorContext.get(\"testCollector\") returns the same SimpleCollector instance that was added",
      "description": "Verify that a normal collector can be added and retrieved using the same name."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "null"
        },
        {
          "parameter": "collector",
          "value": "new SimpleCollector<>()"
        }
      ],
      "expected": "CollectorContext.get(null) returns the SimpleCollector instance (HashMap permits null keys)",
      "description": "Check behaviour when the name argument is null – the map should store the entry under a null key without throwing."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"nullCollector\""
        },
        {
          "parameter": "collector",
          "value": "null"
        }
      ],
      "expected": "CollectorContext.get(\"nullCollector\") returns null",
      "description": "Validate that adding a null collector stores a null value for the given name."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"duplicate\""
        },
        {
          "parameter": "collector",
          "value": "new SimpleCollector<>(\"first\")"
        }
      ],
      "expected": "After first add, get(\"duplicate\") returns the first collector; after second add with a different collector, get(\"duplicate\") returns the second collector",
      "description": "Ensure that adding a collector with an existing name overwrites the previous entry."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"c1\""
        },
        {
          "parameter": "collector",
          "value": "new SimpleCollector<>(\"c1\")"
        },
        {
          "parameter": "name2",
          "value": "\"c2\""
        },
        {
          "parameter": "collector2",
          "value": "new SimpleCollector<>(\"c2\")"
        }
      ],
      "expected": "CollectorContext.getAll().size() == 2 and each name maps to its respective collector",
      "description": "Add two distinct collectors and verify that both are present and the map size reflects the number of distinct keys."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"genericString\""
        },
        {
          "parameter": "collector",
          "value": "new SimpleCollector<String>()"
        }
      ],
      "expected": "CollectorContext.get(\"genericString\") returns a Collector<String> instance",
      "description": "Confirm that the generic type parameter does not affect the storage – a Collector<String> can be stored and retrieved."
    },
    {
      "input": [
        {
          "parameter": "name",
          "value": "\"genericInteger\""
        },
        {
          "parameter": "collector",
          "value": "new SimpleCollector<Integer>()"
        }
      ],
      "expected": "CollectorContext.get(\"genericInteger\") returns a Collector<Integer> instance",
      "description": "Same as previous test but with a different generic type to prove type‑erasure handling."
    }
  ]
}