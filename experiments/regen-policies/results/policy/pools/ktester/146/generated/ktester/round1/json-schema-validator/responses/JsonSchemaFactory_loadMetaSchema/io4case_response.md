{
  "testCases": [
    {
      "group": "metaSchemaFactory is non‑null – null iri, non‑null config",
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
          "expected": "returns the JsonMetaSchema instance produced by the injected JsonMetaSchemaFactory mock when iri is null and config is non‑null",
          "description": "A custom JsonMetaSchemaFactory mock is injected. The mock is configured to return a specific JsonMetaSchema when called with a null IRI and a non‑null SchemaValidatorsConfig. The test verifies that loadMetaSchema forwards the null IRI unchanged and returns the mock's result."
        }
      ]
    },
    {
      "group": "metaSchemaFactory is non‑null – non‑null iri, null config",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/nullable-config\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns the JsonMetaSchema instance produced by the injected JsonMetaSchemaFactory mock when config is null",
          "description": "The injected factory mock expects a non‑null IRI and a null config. The test asserts that loadMetaSchema passes the null config unchanged and returns the mock's JsonMetaSchema."
        }
      ]
    },
    {
      "group": "metaSchemaFactory is null – null iri, non‑null config",
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
          "expected": "returns the JsonMetaSchema instance produced by DefaultJsonMetaSchemaFactory when iri is null and config is non‑null",
          "description": "The default factory singleton is replaced (via reflection) with a mock that returns a known JsonMetaSchema for a null IRI and a non‑null config. The test confirms that loadMetaSchema correctly falls back to the default factory and returns the mock result."
        }
      ]
    },
    {
      "group": "metaSchemaFactory is null – both iri and config null",
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
          "expected": "returns the JsonMetaSchema instance produced by DefaultJsonMetaSchemaFactory when both arguments are null",
          "description": "A mock DefaultJsonMetaSchemaFactory is installed. It is set up to return a distinct JsonMetaSchema when called with null IRI and null config. The test verifies that loadMetaSchema forwards both nulls to the default factory without throwing NPE."
        }
      ]
    },
    {
      "group": "metaSchemaFactory is non‑null – factory returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/return-null\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "null",
          "description": "The injected JsonMetaSchemaFactory mock is programmed to return null for the given arguments. The test ensures that loadMetaSchema propagates the null result rather than throwing an exception."
        }
      ]
    },
    {
      "group": "metaSchemaFactory is null – default factory returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"urn:jsonschema:meta-null\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "The DefaultJsonMetaSchemaFactory singleton is replaced with a mock that returns null for the supplied IRI and config. The test confirms that loadMetaSchema returns null when the fallback factory yields null."
        }
      ]
    },
    {
      "group": "metaSchemaFactory is non‑null – factory throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/exception\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "RuntimeException propagated",
          "description": "The custom JsonMetaSchemaFactory mock throws a RuntimeException when its getMetaSchema method is invoked. The test asserts that loadMetaSchema does not catch the exception and the same RuntimeException propagates to the caller."
        }
      ]
    }
  ]
}