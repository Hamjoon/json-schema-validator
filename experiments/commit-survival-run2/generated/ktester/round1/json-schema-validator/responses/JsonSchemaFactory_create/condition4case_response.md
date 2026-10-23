{
  "testCases": [
    {
      "group": "SchemaPath handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "null"
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"string\\\"}\")"
            },
            {
              "parameter": "parentSchema",
              "value": "new DummyParentSchema(URI.create(\"http://example.com/base\"))"
            }
          ],
          "expected": "doCreate is invoked with schemaPath = \"#\" (default) and returns the dummy JsonSchema instance",
          "description": "When the supplied schemaPath argument is null, create() should replace it with \"#\" before delegating to doCreate."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"/my/schema/path\""
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"integer\\\"}\")"
            },
            {
              "parameter": "parentSchema",
              "value": "new DummyParentSchema(URI.create(\"http://example.com/base\"))"
            }
          ],
          "expected": "doCreate is invoked with schemaPath = \"/my/schema/path\" (unchanged) and returns the dummy JsonSchema instance",
          "description": "When schemaPath is non‑null, create() must forward the exact value to doCreate."
        }
      ]
    },
    {
      "group": "Null parent schema handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"/any\""
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"boolean\\\"}\")"
            },
            {
              "parameter": "parentSchema",
              "value": "null"
            }
          ],
          "expected": "NullPointerException is thrown because parentSchema.getCurrentUri() is accessed",
          "description": "create() does not guard against a null parentSchema; invoking it with null should result in NPE."
        }
      ]
    },
    {
      "group": "Null schemaNode forwarding",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"/path\""
            },
            {
              "parameter": "schemaNode",
              "value": "null"
            },
            {
              "parameter": "parentSchema",
              "value": "new DummyParentSchema(URI.create(\"http://example.com/base\"))"
            }
          ],
          "expected": "doCreate is invoked with schemaNode = null and returns the dummy JsonSchema instance",
          "description": "The method should forward a null JsonNode unchanged; the test verifies that doCreate receives null."
        }
      ]
    }
  ]
}