{
  "testCases": [
    {
      "group": "Cache miss – load new meta‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/meta-schema.json\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "A new JsonMetaSchema instance is returned. The internal metaSchemas map now contains an entry with key equal to normalizeMetaSchemaUri(\"http://example.com/schema/meta-schema.json\"). The loadMetaSchema(String, SchemaValidatorsConfig) method is invoked exactly once with the normalized key and the supplied config.",
          "description": "When the IRI is not present in the cache, getMetaSchema must compute the normalized key, call loadMetaSchema, store the result and return it."
        }
      ]
    },
    {
      "group": "Cache hit – reuse existing meta‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/meta-schema.json\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "The previously cached JsonMetaSchema instance (created in the previous test) is returned. loadMetaSchema is NOT invoked again.",
          "description": "If the normalized IRI already exists in the metaSchemas map, computeIfAbsent must return the existing value without calling loadMetaSchema."
        }
      ]
    },
    {
      "group": "Normalization effect – different IRIs map to same key",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/meta-schema\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "The same JsonMetaSchema instance as for \"http://example.com/schema/meta-schema.json\" is returned because normalizeMetaSchemaUri strips trailing \".json\" (or adds a default fragment). loadMetaSchema is NOT called the second time.",
          "description": "verify that normalizeMetaSchemaUri produces identical keys for equivalent IRIs, causing cache reuse."
        }
      ]
    },
    {
      "group": "Null IRI handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown (or IllegalArgumentException depending on implementation) because normalizeMetaSchemaUri does not accept null.",
          "description": "Ensure that passing a null IRI results in a predictable exception rather than silent failure."
        }
      ]
    },
    {
      "group": "Config propagation – null config",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"urn:jsonschema:draft-07\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "A JsonMetaSchema is returned; loadMetaSchema receives null as its config argument. No NullPointerException occurs inside loadMetaSchema.",
          "description": "Validate that a null SchemaValidatorsConfig is forwarded correctly to loadMetaSchema when a new meta‑schema is created."
        }
      ]
    }
  ]
}