{
  "testCases": [
    {
      "group": "metaSchemaFactory is non‑null – delegate to custom factory",
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
          "expected": "returns the JsonMetaSchema instance produced by the injected JsonMetaSchemaFactory mock",
          "description": "When JsonSchemaFactory.metaSchemaFactory is set, loadMetaSchema should call metaSchemaFactory.getMetaSchema(iri, this, config) and return its result. The test injects a mock factory that returns a predefined JsonMetaSchema and verifies the returned object is exactly that instance."
        },
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
          "expected": "returns the JsonMetaSchema instance produced by the injected JsonMetaSchemaFactory mock even when both arguments are null",
          "description": "Ensures the method forwards null arguments unchanged to the custom factory. The mock is configured to return a special JsonMetaSchema when called with null values; the test asserts that this object is returned."
        }
      ]
    },
    {
      "group": "metaSchemaFactory is null – fallback to DefaultJsonMetaSchemaFactory",
      "cases": [
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"http://example.com/default\""
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "returns the JsonMetaSchema instance produced by DefaultJsonMetaSchemaFactory.getInstance().getMetaSchema",
          "description": "When JsonSchemaFactory.metaSchemaFactory is null, loadMetaSchema should obtain the singleton DefaultJsonMetaSchemaFactory and delegate to its getMetaSchema method. The test replaces the singleton (using a test‑only setter or reflection) with a mock that returns a known JsonMetaSchema and verifies the returned object."
        },
        {
          "input": [
            {
              "parameter": "iri",
              "value": "\"urn:jsonschema:meta\""
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "returns the JsonMetaSchema instance produced by DefaultJsonMetaSchemaFactory even when config is null",
          "description": "Verifies that a null SchemaValidatorsConfig is passed through to the default factory without causing NPEs. The mocked DefaultJsonMetaSchemaFactory returns a distinct JsonMetaSchema for this call, which the test asserts."
        }
      ]
    }
  ]
}