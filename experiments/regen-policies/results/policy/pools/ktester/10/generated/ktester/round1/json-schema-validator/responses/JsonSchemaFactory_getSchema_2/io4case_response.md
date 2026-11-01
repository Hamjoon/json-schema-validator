{
  "group": "JsonSchemaFactory.getSchema(InputStream) - additional edge cases",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream((\"\\uFEFF{\" + \"\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\",\\\"type\\\":\\\"object\\\"}\" ).getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "non‑null JsonSchema instance; the UTF‑8 BOM is ignored and the schema is parsed correctly (meta‑schema id = \"http://json-schema.org/draft-07/schema#\", root type = OBJECT)",
      "description": "Validate that a stream beginning with a UTF‑8 Byte Order Mark does not break parsing and the factory still returns a proper JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(\"   \\n\\t{ \\\"$schema\\\": \\\"http://json-schema.org/draft-07/schema#\\\", \\\"type\\\": \\\"object\\\" }\".getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "non‑null JsonSchema instance; leading whitespace is tolerated and the schema is parsed successfully",
      "description": "Ensure that the method can handle input streams that contain insignificant leading whitespace or line‑breaks before the JSON document."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new InputStream() {\n"         

            , "value": "new InputStream() {\n"           
        }
      ],
      "expected": "JsonSchemaException (or RuntimeException) wrapping an IOException because the underlying stream fails during read",
      "description": "Simulate a failing InputStream that throws IOException on read; the factory should propagate the error wrapped in a JsonSchemaException."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-04/schema#\\\",\\\"type\\\":\\\"object\\\"}\".getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema id is \"http://json-schema.org/draft-04/schema#\" (draft‑04 is recognised and loaded)",
      "description": "Check that the factory correctly recognises a $schema reference to Draft‑04 and loads the appropriate meta‑schema."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(\"{\\\"$id\\\":\\\"http://example.com/my-schema#\\\",\\\"type\\\":\\\"object\\\"}\".getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "non‑null JsonSchema instance; schema.getSchemaLocation().toString() equals \"http://example.com/my-schema#\"",
      "description": "Verify that when a schema defines an explicit $id, the resulting JsonSchema carries that identifier as its SchemaLocation."
    }
  ]
}