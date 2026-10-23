{
  "group": "CollectorContext.add(String, Object) - basic functionality and edge cases",
  "cases": [
    {
      "input": [
        { "parameter": "name", "value": "\"firstKey\"" },
        { "parameter": "object", "value": "\"firstValue\"" }
      ],
      "expected": "collectorMap contains key \"firstKey\" with value \"firstValue\"; get(\"firstKey\") returns \"firstValue\" and map size is 1",
      "description": "Add a normal non‑null key/value pair and verify it is stored correctly."
    },
    {
      "input": [
        { "parameter": "name", "value": "null" },
        { "parameter": "object", "value": "\"nullKeyValue\"" }
      ],
      "expected": "collectorMap contains a null key with value \"nullKeyValue\"; get(null) returns \"nullKeyValue\" and map size is 1",
      "description": "Add an entry with a null key – HashMap permits null keys, so the method should store it without throwing."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"nullValueKey\"" },
        { "parameter": "object", "value": "null" }
      ],
      "expected": "collectorMap contains key \"nullValueKey\" with a null value; get(\"nullValueKey\") returns null and map size is 1",
      "description": "Add an entry with a null value – HashMap permits null values, so the method should store it."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"duplicateKey\"" },
        { "parameter": "object", "value": "\"firstObject\"" }
      ],
      "expected": "collectorMap contains key \"duplicateKey\" with value \"firstObject\"; map size is 1",
      "description": "First insertion of a key to establish baseline."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"duplicateKey\"" },
        { "parameter": "object", "value": "\"secondObject\"" }
      ],
      "expected": "collectorMap still has size 1 and get(\"duplicateKey\") returns \"secondObject\" (value overwritten)",
      "description": "Add a second entry with the same key but a different object; the previous value should be replaced without increasing map size."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"key1\"" },
        { "parameter": "object", "value": "123" }
      ],
      "expected": "collectorMap contains key \"key1\" with Integer value 123; map size reflects total distinct keys added",
      "description": "Add an entry where the value is a non‑String object (Integer) to verify generic handling."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"key2\"" },
        { "parameter": "object", "value": "new java.util.ArrayList<>()" }
      ],
      "expected": "collectorMap contains key \"key2\" with an empty ArrayList instance; map size reflects total distinct keys added",
      "description": "Add an entry where the value is a collection object to ensure any Object type is accepted."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"key3\"" },
        { "parameter": "object", "value": "new com.networknt.schema.CollectorContext()" }
      ],
      "expected": "collectorMap contains key \"key3\" with a CollectorContext instance; map size reflects total distinct keys added",
      "description": "Add an entry where the value is another CollectorContext instance (self‑reference) to test handling of complex objects."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"bulkKey1\"" },
        { "parameter": "object", "value": "\"value1\"" }
      ],
      "expected": "collectorMap contains key \"bulkKey1\" with value \"value1\"",
      "description": "First of a series of bulk adds – will be followed by additional adds to verify that multiple distinct entries coexist."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"bulkKey2\"" },
        { "parameter": "object", "value": "\"value2\"" }
      ],
      "expected": "collectorMap contains key \"bulkKey2\" with value \"value2\" and total size is 3 (including previous keys)",
      "description": "Second bulk add, confirming that map size increments correctly with each new distinct key."
    },
    {
      "input": [
        { "parameter": "name", "value": "\"bulkKey3\"" },
        { "parameter": "object", "value": "\"value3\"" }
      ],
      "expected": "collectorMap contains key \"bulkKey3\" with value \"value3\" and total size is 4 (including previous keys)",
      "description": "Third bulk add, final verification that all previously added entries remain unchanged."
    }
  ]
}