{
  "tests": [
    {
      "group": "Load meta‑schema when absent",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns a JsonMetaSchema instance created by loadMetaSchema; loadMetaSchema is invoked exactly once with normalized key \"http://example.com/schema\" and the supplied config",
          "description": "The factory does not contain the normalized IRI; computeIfAbsent should call the lambda, which delegates to loadMetaSchema. The returned object must be the one produced by the overridden loadMetaSchema."
        }
      ]
    },
    {
      "group": "Return cached meta‑schema when present",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns the previously cached JsonMetaSchema instance; loadMetaSchema is NOT invoked for this call",
          "description": "The map already contains an entry for the normalized IRI. computeIfAbsent must return the existing value without executing the loader lambda."
        }
      ]
    },
    {
      "group": "Normalization leads to same cache key",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/schema/\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "first call loads a new JsonMetaSchema (loader invoked); second call with \"http://example.com/schema\" returns the same instance and loader is not invoked again",
          "description": "normalizeMetaSchemaUri should strip trailing slashes (or otherwise produce a canonical form). Two different textual IRIs map to the same cache key, proving that the normalization step works."
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
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws NullPointerException (or IllegalArgumentException if implementation validates) before calling loadMetaSchema",
          "description": "Passing a null IRI should cause the method to fail early because normalizeMetaSchemaUri cannot process null. The test verifies that the exception is thrown and no loader call occurs."
        }
      ]
    },
    {
      "group": "Null config is accepted",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/another\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns a JsonMetaSchema instance created by loadMetaSchema with config argument null",
          "description": "The method should forward a null SchemaValidatorsConfig to loadMetaSchema. The test confirms that the returned schema is produced and no NullPointerException is raised."
        }
      ]
    }
  ]
}