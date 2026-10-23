{
  "testCases": [
    {
      "group": "ValidationContext null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "null"
            },
            {
              "parameter": "schemaPath",
              "value": "\"/valid/path\""
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"number\\\"}\")"
            },
            {
              "parameter": "parentSchema",
              "value": "new DummyParentSchema(URI.create(\"http://example.com/base\"))"
            }
          ],
          "expected": "doCreate is invoked with validationContext = null and returns the dummy JsonSchema instance",
          "description": "The method should forward a null ValidationContext unchanged; no NullPointerException should be thrown by create() itself."
        },
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"/valid/path\""
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"number\\\"}\")"
            },
            {
              "parameter": "parentSchema",
              "value": "new DummyParentSchema(URI.create(\"http://example.com/base\"))"
            }
          ],
          "expected": "doCreate is invoked with the same ValidationContext instance that was supplied",
          "description": "When a non‑null ValidationContext is supplied, create() must forward the exact instance to doCreate."
        }
      ]
    },
    {
      "group": "Empty schemaPath handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"\""
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
          "expected": "doCreate is invoked with schemaPath = \"\" (empty string) and returns the dummy JsonSchema instance",
          "description": "An empty string is a valid schemaPath value; create() must forward it unchanged (it only replaces null)."
        }
      ]
    },
    {
      "group": "ParentSchema with null currentUri",
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
              "value": "new DummyParentSchemaWithNullUri()"
            }
          ],
          "expected": "doCreate is invoked with currentUri = null and returns the dummy JsonSchema instance",
          "description": "If the parent schema's getCurrentUri() returns null, create() should still forward the null value to doCreate without throwing an exception."
        }
      ]
    },
    {
      "group": "Complex schemaNode forwarding",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"/complex\""
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"name\\\":{\\\"type\\\":\\\"string\\\"},\\\"age\\\":{\\\"type\\\":\\\"integer\\\"}}}\")"
            },
            {
              "parameter": "parentSchema",
              "value": "new DummyParentSchema(URI.create(\"http://example.com/base\"))"
            }
          ],
          "expected": "doCreate receives the same ObjectNode instance representing the complex schema and returns the dummy JsonSchema",
          "description": "create() must forward complex (object) JsonNode structures unchanged to doCreate."
        }
      ]
    },
    {
      "group": "SchemaPath containing fragment identifier",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/definitions/Person\""
            },
            {
              "parameter": "schemaNode",
              "value": "new ObjectMapper().readTree(\"{\\\"$ref\\\":\\\"#/definitions/Person\\\"}\")"
            },
            {
              "parameter": "parentSchema",
              "value": "new DummyParentSchema(URI.create(\"http://example.com/schemas/base.json\"))"
            }
          ],
          "expected": "doCreate is invoked with schemaPath = \"#/definitions/Person\" unchanged and returns the dummy JsonSchema instance",
          "description": "When the supplied schemaPath contains a JSON‑Schema fragment, create() must not alter it before delegating to doCreate."
        }
      ]
    }
  ]
}