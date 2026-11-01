{
  "testCases": [
    {
      "group": "Custom configuration",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "validJsonSchemaStream"
            },
            {
              "parameter": "config",
              "value": "customConfig{typeLoose:true,failFast:true}"
            }
          ],
          "expected": "instance of JsonSchema (custom configuration applied)",
          "description": "A well‑formed JSON schema stream together with a non‑default SchemaValidatorsConfig (e.g., typeLoose and failFast enabled). The factory must return a JsonSchema object that has been created using the supplied configuration."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "validJsonSchemaStreamWithBOM"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "instance of JsonSchema",
          "description": "The InputStream starts with a UTF‑8 Byte Order Mark (BOM) followed by a valid JSON schema. The method should ignore the BOM and successfully parse the schema."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "validJsonSchemaStreamWithTrailingWhitespace"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "instance of JsonSchema (default configuration applied)",
          "description": "A valid JSON schema that contains extra whitespace characters after the closing brace. With a null config the factory falls back to its default configuration and must still return a JsonSchema."
        }
      ]
    },
    {
      "group": "Edge stream conditions",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "emptyStream"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "IOException or JsonProcessingException indicating empty content",
          "description": "An empty InputStream with a null config should behave the same as with a non‑null config: the method must propagate an exception because there is no content to parse."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "invalidJsonStream"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "IOException or JsonProcessingException indicating malformed JSON",
          "description": "A stream containing syntactically invalid JSON together with a null config must still result in a parsing exception."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "yamlSchemaStream"
            },
            {
              "parameter": "config",
              "value": null
            }
          ],
          "expected": "IOException or JsonProcessingException because the default InputFormat is JSON",
          "description": "Supplying a YAML document while the method forces InputFormat.JSON and passing a null config should still cause a parsing failure."
        }
      ]
    },
    {
      "group": "Exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ioExceptionThrowingStream"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "IOException propagated from underlying stream",
          "description": "The InputStream throws an IOException during read(). The method must not swallow the exception; it should propagate it to the caller."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "closedStream"
            },
            {
              "parameter": "config",
              "value": "defaultConfig"
            }
          ],
          "expected": "IOException indicating stream is closed",
          "description": "The InputStream has already been closed before being passed to getSchema. Attempting to read should result in an IOException that is propagated."
        }
      ]
    }
  ]
}