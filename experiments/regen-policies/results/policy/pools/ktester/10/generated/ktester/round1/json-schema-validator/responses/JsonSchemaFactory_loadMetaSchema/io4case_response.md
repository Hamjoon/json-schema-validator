{
  "tests": [
    {
      "group": "Null SchemaValidatorsConfig (should default to empty config)",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "\"http://example.com/null-config-meta\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "JsonMetaSchema built with the given id and without any vocabularies (builder’s `vocabularies` remains null). The method must treat a null `config` as if a new `SchemaValidatorsConfig()` were supplied.",
          "description": "Pass `null` for the `SchemaValidatorsConfig`. The method should not throw NPE and should behave the same as when an empty config is supplied."
        }
      ]
    },
    {
      "group": "Null id argument (should raise IllegalArgumentException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) is thrown because the `id` argument is required for locating the meta‑schema.",
          "description": "Verify that the method validates its `id` parameter and fails fast when it is `null`."
        }
      ]
    },
    {
      "group": "$vocabulary present with non‑boolean values",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "\"http://example.com/non‑boolean‑vocab\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonMetaSchema built with a vocabularies map where each entry’s value is the result of `JsonNode.booleanValue()`. Non‑boolean nodes (e.g., numbers, strings) are coerced to `false`. For a schema containing `$vocabulary:{\"core\":1,\"applicator\":\"yes\"}` the resulting map is {\"core\":false,\"applicator\":false}.",
          "description": "When the meta‑schema version is V201909 or newer, the method iterates over the `$vocabulary` object regardless of the actual JSON node types. This test ensures that non‑boolean values are handled gracefully (converted to `false`)."
        }
      ]
    },
    {
      "group": "$vocabulary present but empty object",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "\"http://example.com/empty‑vocab\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonMetaSchema built with an empty vocabularies map (size 0). The builder’s `vocabularies` is non‑null but contains no entries.",
          "description": "A `$vocabulary` node exists but has no properties. The method should still initialise the map (because the version check passes) and leave it empty."
        }
      ]
    },
    {
      "group": "$vocabulary entries with explicit null values",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "\"http://example.com/null‑value‑vocab\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonMetaSchema built with a vocabularies map where null entries are interpreted as `false`. For `$vocabulary:{\"core\":null}` the resulting map is {\"core\":false}.",
          "description": "Ensures that a `$vocabulary` property whose value is JSON `null` does not cause an exception and is treated as `false` by `booleanValue()`."
        }
      ]
    },
    {
      "group": "Specification exactly V201909 (boundary condition)",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "\"http://example.com/boundary‑v201909\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "JsonMetaSchema built with vocabularies map populated from `$vocabulary` when present, because the version flag value is equal to V201909. If the schema contains `$vocabulary:{\"core\":true}` the map will be {\"core\":true}.",
          "description": "Tests the inclusive comparison (`>=`) used in the version check. The method must treat V201909 as “new enough” to process vocabularies."
        }
      ]
    }
  ]
}