{
  "testCases": [
    {
      "group": "Successful schema loading",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance whose root node matches the supplied JSON schema.",
          "description": "Valid JSON schema string with JSON input format and a default configuration should be parsed successfully and wrapped in a JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: object\nproperties:\n  age:\n    type: integer"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "config",
              "value": "null"
            }
          ],
          "expected": "Returns a non‑null JsonSchema instance; the internal JsonNode reflects the YAML schema converted to JSON.",
          "description": "Valid YAML schema string with YAML input format and a null configuration should be parsed successfully (the factory will create a default config internally)."
        }
      ]
    },
    {
      "group": "Exception handling (IOException path)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"invalid\": [ }"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException wrapping an IOException caused by malformed JSON.",
          "description": "Malformed JSON schema triggers an IOException inside readTree; the method must catch it, log the error, and re‑throw a JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException wrapping an IOException (or NullPointerException) because readTree cannot process a null content string.",
          "description": "Passing a null schema string should result in an exception path; the factory must convert the low‑level exception into a JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\" }"
            },
            {
              "parameter": "inputFormat",
              "value": null
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "Throws JsonSchemaException wrapping an IOException (or IllegalArgumentException) because the input format is undefined.",
          "description": "When the InputFormat argument is null the internal readTree method cannot decide how to deserialize; this should be caught and re‑thrown as a JsonSchemaException."
        }
      ]
    }
  ]
}