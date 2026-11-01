{
  "testCases": [
    {
      "group": "loadMetaSchema – default factory throws IllegalArgumentException for malformed IRI",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"not-a-valid-uri\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IllegalArgumentException propagated from DefaultJsonMetaSchemaFactory.getInstance().getMetaSchema",
          "description": "When the JsonSchemaFactory has no custom metaSchemaFactory, the fallback default factory validates the IRI format and throws IllegalArgumentException for a malformed IRI. The method should not catch it."
        }
      ]
    },
    {
      "group": "loadMetaSchema – default factory throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/runtime-error\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws RuntimeException (e.g., IllegalStateException) propagated from DefaultJsonMetaSchemaFactory",
          "description": "Simulates a situation where the default factory encounters an unexpected condition and throws a RuntimeException. The loadMetaSchema method must let the exception bubble up."
        }
      ]
    },
    {
      "group": "loadMetaSchema – default factory returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/null-default\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns null (the value returned by DefaultJsonMetaSchemaFactory.getInstance().getMetaSchema)",
          "description": "Ensures that loadMetaSchema does not perform a null‑check on the result of the default factory; if the factory returns null, the method returns null as‑is."
        }
      ]
    },
    {
      "group": "loadMetaSchema – custom factory throws IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/illegal-arg\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws IllegalArgumentException propagated from the injected JsonMetaSchemaFactory mock",
          "description": "The custom JsonMetaSchemaFactory is programmed to reject the supplied IRI by throwing IllegalArgumentException. loadMetaSchema must not swallow the exception."
        }
      ]
    },
    {
      "group": "loadMetaSchema – custom factory throws NullPointerException due to null config",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/null-config\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException propagated from the custom JsonMetaSchemaFactory mock",
          "description": "Even though loadMetaSchema forwards a null config, the custom factory may dereference it and throw NPE. The method should let the exception propagate."
        }
      ]
    },
    {
      "group": "loadMetaSchema – custom factory throws Error (AssertionError)",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/assertion-error\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws AssertionError propagated from the injected JsonMetaSchemaFactory mock",
          "description": "Verifies that loadMetaSchema does not catch Errors; an AssertionError thrown by the custom factory should bubble up unchanged."
        }
      ]
    }
  ]
}