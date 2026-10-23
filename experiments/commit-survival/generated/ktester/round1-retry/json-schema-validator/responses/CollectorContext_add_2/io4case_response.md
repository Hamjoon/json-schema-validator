{
  "testCases": [
    {
      "group": "CollectorContext.add(String, Object) - additional edge cases",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "\"\"" },
            { "parameter": "object", "value": "\"emptyKeyValue\"" }
          ],
          "expected": "collectorMap contains key \"\" with value \"emptyKeyValue\"; get(\"\") returns \"emptyKeyValue\"; map size is 1",
          "description": "Add an entry with an empty string as the key to verify that empty keys are accepted."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"   \"" },
            { "parameter": "object", "value": "\"whitespaceKeyValue\"" }
          ],
          "expected": "collectorMap contains key \"   \" with value \"whitespaceKeyValue\"; get(\"   \") returns \"whitespaceKeyValue\"; map size is 1",
          "description": "Add an entry whose key consists only of whitespace characters."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"!@#$%^&*()_+\"" },
            { "parameter": "object", "value": "\"specialCharsValue\"" }
          ],
          "expected": "collectorMap contains key \"!@#$%^&*()_+\" with value \"specialCharsValue\"; get(\"!@#$%^&*()_+\") returns \"specialCharsValue\"; map size is 1",
          "description": "Add an entry with a key containing various special characters."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"ключ\"" },
            { "parameter": "object", "value": "\"unicodeValue\"" }
          ],
          "expected": "collectorMap contains key \"ключ\" with value \"unicodeValue\"; get(\"ключ\") returns \"unicodeValue\"; map size is 1",
          "description": "Add an entry with a Unicode (Cyrillic) key to ensure Unicode handling."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"%s\".repeat(10000)" },
            { "parameter": "object", "value": "\"longKeyValue\"" }
          ],
          "expected": "collectorMap contains the very‑long key (10,000 characters) with value \"longKeyValue\"; get(veryLongKey) returns \"longKeyValue\"; map size is 1",
          "description": "Add an entry with an extremely long key string to test memory handling and hashing."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"largeListKey\"" },
            { "parameter": "object", "value": "new java.util.ArrayList<>(java.util.Collections.nCopies(10000, 1))" }
          ],
          "expected": "collectorMap contains key \"largeListKey\" with a List of size 10,000; get(\"largeListKey\").size() == 10000; map size is 1",
          "description": "Add an entry where the value is a large collection to verify that the method accepts big objects."
        },
        {
          "input": [
            { "parameter": "name", "value": "null" },
            { "parameter": "object", "value": "null" }
          ],
          "expected": "collectorMap contains a null key with a null value; get(null) returns null; map size is 1",
          "description": "Add an entry where both key and value are null – HashMap permits both."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"idempotentKey\"" },
            { "parameter": "object", "value": "\"sameValue\"" }
          ],
          "expected": "collectorMap size after first add is 1; after second add with identical key/value size remains 1 and get(\"idempotentKey\") still returns \"sameValue\"",
          "description": "Add the same key/value pair twice; map size should not increase."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"overwriteWithNull\"" },
            { "parameter": "object", "value": "\"initialValue\"" }
          ],
          "expected": "collectorMap size after first add is 1; after second add with same key and null value size stays 1 and get(\"overwriteWithNull\") returns null",
          "description": "Overwrite an existing non‑null value with null to verify that null values replace previous ones without changing size."
        },
        {
          "input": [
            { "parameter": "name", "value": "\"sharedKey\"" },
            { "parameter": "object", "value": "\"valueInFirstContext\"" }
          ],
          "expected": "first CollectorContext instance map size is 1 with key \"sharedKey\"; second CollectorContext instance map size is 0 before its own add; after adding same key to second instance, its map size is 1 with its own value; maps are independent.",
          "description": "Create two separate CollectorContext objects and add the same key to each; ensure they maintain independent internal maps."
        }
      ]
    }
  ]
}