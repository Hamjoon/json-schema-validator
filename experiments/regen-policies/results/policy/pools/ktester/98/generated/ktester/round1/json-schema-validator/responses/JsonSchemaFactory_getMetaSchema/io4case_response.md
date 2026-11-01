{
  "testCases": [
    {
      "group": "Cache miss – custom SchemaValidatorsConfig is propagated",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/custom-meta-schema.json\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setTypeLoose(false)"
            }
          ],
          "expected": "A new JsonMetaSchema instance is returned. The loadMetaSchema method is invoked exactly once with the normalized IRI key and the supplied custom SchemaValidatorsConfig instance. The returned JsonMetaSchema holds the configuration passed (e.g., typeLoose = false).",
          "description": "When the IRI is not cached and a non‑default SchemaValidatorsConfig is supplied, getMetaSchema must forward that exact config to loadMetaSchema and store the resulting meta‑schema."
        }
      ]
    },
    {
      "group": "Cache hit – different config ignored after first load",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/custom-meta-schema.json\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "The JsonMetaSchema instance created in the previous test is returned. loadMetaSchema is NOT invoked again, even though a different (null) config is supplied. The configuration of the cached instance remains the one from the first call.",
          "description": "Ensures that after a meta‑schema has been cached, subsequent calls with the same normalized IRI ignore the new config argument and reuse the cached object."
        }
      ]
    },
    {
      "group": "Fragment identifier – same key as base IRI",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/meta-schema.json#\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "The JsonMetaSchema instance that is already cached for the base IRI \"http://example.com/schema/meta-schema.json\" is returned. No new loadMetaSchema call occurs. The fragment part is ignored by normalizeMetaSchemaUri.",
          "description": "Verifies that a trailing fragment identifier does not affect the cache key, causing reuse of the existing meta‑schema."
        }
      ]
    },
    {
      "group": "Case‑insensitivity of scheme – normalized key is lower‑cased",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"HTTP://example.com/schema/meta-schema.json\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "The same JsonMetaSchema instance as for the lower‑case IRI \"http://example.com/schema/meta-schema.json\" is returned. No additional loadMetaSchema call is made. normalizeMetaSchemaUri normalises the scheme to lower case.",
          "description": "Checks that the scheme component of the IRI is treated case‑insensitively during normalization, leading to cache reuse."
        }
      ]
    },
    {
      "group": "Empty string IRI – illegal argument handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException if implementation treats empty as null) is thrown by normalizeMetaSchemaUri.",
          "description": "Ensures that an empty IRI string is rejected rather than being silently accepted."
        }
      ]
    },
    {
      "group": "Malformed IRI – parsing failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"ht!tp://[invalid]\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException (or URISyntaxException wrapped in RuntimeException) is thrown when normalizeMetaSchemaUri attempts to validate the malformed IRI.",
          "description": "Validates that getMetaSchema does not accept syntactically invalid IRIs and propagates an appropriate exception."
        }
      ]
    },
    {
      "group": "Trailing slash – treated as same meta‑schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/meta-schema.json/\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "The cached JsonMetaSchema for \"http://example.com/schema/meta-schema.json\" is returned. The trailing slash is stripped by normalizeMetaSchemaUri, so no new loadMetaSchema call occurs.",
          "description": "Confirms that a superfluous trailing slash does not create a distinct cache entry."
        }
      ]
    },
    {
      "group": "Concurrent access – loadMetaSchema invoked only once",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/concurrent-meta-schema.json\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "When getMetaSchema is called from multiple threads simultaneously with the same IRI, the internal ConcurrentMap.computeIfAbsent guarantees that loadMetaSchema is executed exactly once. All threads receive the same JsonMetaSchema instance.",
          "description": "Simulates concurrent calls (e.g., using an ExecutorService) to verify the thread‑safety of the cache mechanism."
        }
      ]
    }
  ]
}