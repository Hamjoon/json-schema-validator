{
  "testCases": [
    {
      "group": "NullInputStream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling getSchema(null) should throw a NullPointerException because the method delegates to getSchema(InputStream, null) which expects a non‑null stream."
        }
      ]
    },
    {
      "group": "EmptyInputStream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(new byte[0])"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the schema content is empty",
          "description": "An empty InputStream does not contain any JSON/YAML. The factory should detect the lack of content and raise an exception rather than returning a valid JsonSchema."
        }
      ]
    },
    {
      "group": "InvalidJson",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(\"{ invalid json }\".getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "JsonProcessingException wrapped in a JsonSchemaException",
          "description": "When the stream contains malformed JSON, the internal ObjectMapper/YAMLMapper parsing fails and the method should propagate a parsing exception."
        }
      ]
    },
    {
      "group": "ValidJsonWithoutConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(\"{ \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema#\\\", \\\"type\\\": \\\"object\\\" }\".getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "JsonSchema instance (non‑null) with default validation configuration",
          "description": "A well‑formed JSON schema should be parsed successfully and a JsonSchema object returned. No custom config is supplied, so default validators are used."
        }
      ]
    },
    {
      "group": "ValidJsonWithCustomConfig",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(\"{ \\\"type\\\": \\\"string\\\" }\".getBytes(StandardCharsets.UTF_8))"
            },
            {
              "parameter": "config",
              "value": "new SchemaValidatorsConfig().setFailFast(true)"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose validation context reflects the custom config (e.g., failFast = true)",
          "description": "When a non‑null SchemaValidatorsConfig is supplied, the factory must use it to build the ValidationContext. The returned JsonSchema should honour the custom settings."
        }
      ]
    },
    {
      "group": "ValidYaml",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(\"type: object\\nproperties:\\n  name:\\n    type: string\".getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "JsonSchema instance (non‑null) parsed via YAMLMapper",
          "description": "The factory should detect that the content is YAML (or rely on YAMLMapper) and successfully create a JsonSchema object."
        }
      ]
    },
    {
      "group": "IOExceptionDuringRead",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new InputStream() { @Override public int read() throws IOException { throw new IOException(\"forced I/O error\"); } }"
            }
          ],
          "expected": "IOException wrapped in a JsonSchemaException",
          "description": "If the underlying InputStream throws an IOException while being read, the method must propagate the exception (typically wrapped) rather than silently succeed."
        }
      ]
    },
    {
      "group": "StreamClosedAfterParsing",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(\"{ \\\"type\\\": \\\"null\\\" }\".getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "JsonSchema instance (non‑null) and the InputStream is closed after method returns",
          "description": "The factory should close the provided InputStream after reading the schema. The test verifies that calling schemaStream.read() after getSchema returns -1 (closed)."
        }
      ]
    }
  ]
}