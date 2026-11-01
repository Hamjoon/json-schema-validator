Invalid control character at: line 106 column 54 (char 4343)
{
  "testCases": [
    {
      "group": "JsonSchemaFactory.getSchema(InputStream, InputFormat, SchemaValidatorsConfig) - NullPointerException when InputStream is null (unwrapped)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "null"
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
          "expected": "NullPointerException is thrown directly (not wrapped in JsonSchemaException) because readTree dereferences a null InputStream before the IOException catch block can intercept it.",
          "description": "Verify that a null InputStream causes a runtime NPE which propagates out of the method, confirming that the method does not defensively guard against a null stream before delegating to readTree."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(InputStream, InputFormat, SchemaValidatorsConfig) - NullPointerException when InputFormat is null (unwrapped)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a minimal valid JSON schema: {\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "NullPointerException is thrown directly (not wrapped) because readTree attempts to use a null InputFormat to select a mapper.",
          "description": "Ensure that passing a null InputFormat leads to an immediate NPE from the mapper selection logic, which bypasses the IOException catch block."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(InputStream, InputFormat, SchemaValidatorsConfig) - IOException thrown mid‑read",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "Custom InputStream that returns a few bytes of a valid JSON schema then throws IOException on the next read"
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
          "expected": "JsonSchemaException is thrown with the original IOException as its cause.",
          "description": "Simulate a stream failure after some data has been read to verify that the method catches the IOException, logs it, and re‑throws a JsonSchemaException."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(InputStream, InputFormat, SchemaValidatorsConfig) - IOException on stream close",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "Custom InputStream that reads the whole valid JSON schema successfully but whose close() method throws IOException"
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
          "expected": "JsonSchemaException is thrown with the IOException from close() as its cause.",
          "description": "Validate that an IOException occurring during the cleanup phase (stream close) is also captured by the catch block and wrapped in JsonSchemaException."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchema(InputStream, InputFormat, SchemaValidatorsConfig) - Unsupported InputFormat value",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a minimal valid JSON schema: {\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.valueOf(\"XML\")  // throws IllegalArgumentException before method call"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig()"
            }
          ],
          "expected": "IllegalArgumentException is thrown before getSchema is invoked (invalid enum constant).",
          "description": "Confirm that providing an InputFormat value that does not exist results in an immediate IllegalArgumentException, demonstrating that the method relies on a closed set of enum values."
        }
      ]
    }
  ]
}