{
  "testCases": [
    {
      "group": "loadMetaSchema – custom JsonMetaSchemaFactory present",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/meta\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns the JsonMetaSchema instance that the injected JsonMetaSchemaFactory mock returns from getMetaSchema(iri, factory, config).",
          "description": "Verifies that when the JsonSchemaFactory has a non‑null metaSchemaFactory, loadMetaSchema delegates to that factory and returns the exact object supplied by the mock."
        }
      ]
    },
    {
      "group": "loadMetaSchema – fallback to DefaultJsonMetaSchemaFactory",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/default-meta\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns the JsonMetaSchema instance obtained from DefaultJsonMetaSchemaFactory.getInstance().getMetaSchema(iri, factory, config).",
          "description": "Ensures that when the JsonSchemaFactory is constructed without a custom metaSchemaFactory (i.e., metaSchemaFactory == null), loadMetaSchema falls back to the default singleton factory and returns its result."
        }
      ]
    },
    {
      "group": "loadMetaSchema – null IRI handling",
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
          "expected": "Throws NullPointerException (or propagates the exception thrown by the underlying MetaSchemaFactory).",
          "description": "Checks the method’s behaviour when a null IRI is supplied; the method itself does not guard against null, so the call should result in a NPE from the delegated factory."
        }
      ]
    },
    {
      "group": "loadMetaSchema – null SchemaValidatorsConfig handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/meta\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Returns the JsonMetaSchema instance from the selected factory (custom or default) with a null config argument.",
          "description": "Validates that a null SchemaValidatorsConfig does not cause loadMetaSchema to fail; the null is simply passed through to the underlying factory."
        }
      ]
    }
  ]
}