{
  "testCases": [
    {
      "group": "Valid inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "validJsonSchemaStream"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "instance of JsonSchema representing the supplied schema",
          "description": "When a non‑null InputStream containing a well‑formed JSON schema and a non‑null SchemaValidatorsConfig are supplied, getSchema should parse the stream and return a JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "validJsonSchemaStream"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "instance of JsonSchema (default configuration applied)",
          "description": "If the config argument is null the method should fall back to the factory’s default SchemaValidatorsConfig and still return a JsonSchema."
        }
      ]
    },
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": null
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "NullPointerException",
          "description": "A null InputStream is not acceptable; the method must throw a NullPointerException (or IllegalArgumentException) before attempting to read."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": null
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Both arguments null – the method should still fail fast with a NullPointerException."
        }
      ]
    },
    {
      "group": "Invalid / empty content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "emptyStream"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "IOException or JsonProcessingException indicating empty content",
          "description": "An InputStream with no bytes cannot be parsed into a JSON tree; the method should propagate an IOException (or a Jackson parsing exception)."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "invalidJsonStream"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "IOException or JsonProcessingException indicating malformed JSON",
          "description": "When the stream contains syntactically invalid JSON, getSchema must throw an exception from the underlying JsonNodeReader."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "yamlSchemaStream"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "IOException or JsonProcessingException because the default InputFormat is JSON",
          "description": "Supplying a YAML document while the method forces InputFormat.JSON should result in a parsing failure."
        }
      ]
    }
  ]
}