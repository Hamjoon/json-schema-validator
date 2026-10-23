Invalid control character at: line 10 column 80 (char 252)
{
  "test_cases": [
    {
      "group": "Malformed JSON handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing '{\"type\":\"object\"'  // missing closing brace"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws JsonSchemaException (cause is JsonParseException)",
          "description": "The InputStream contains malformed JSON that cannot be parsed. jsonMapper.readTree throws a JsonParseException (subclass of IOException), which should be caught and wrapped in a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Empty InputStream handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing no bytes (empty stream)"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws JsonSchemaException (cause is EOFException or JsonParseException)",
          "description": "An empty InputStream provides no JSON content. jsonMapper.readTree throws an IOException (typically EOFException or JsonParseException), which must be wrapped in a JsonSchemaException."
        }
      ]
    },
    {
      "group": "IOException during stream read",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "Custom InputStream whose read() method always throws new IOException(\"simulated read error\")"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws JsonSchemaException (cause is IOException)",
          "description": "The InputStream fails while being read, throwing an IOException. The method should catch this exception and re‑throw it as a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Runtime exception from newJsonSchema propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing '{\"type\":\"object\"}'"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "throws NullPointerException (not wrapped)",
          "description": "Using a subclass of JsonSchemaFactory that overrides newJsonSchema to throw a NullPointerException. Since the method only catches IOException, the NullPointerException should propagate unchanged."
        }
      ]
    }
  ]
}