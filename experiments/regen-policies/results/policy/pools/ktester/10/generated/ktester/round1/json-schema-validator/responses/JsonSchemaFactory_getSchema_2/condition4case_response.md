{
  "group": "JsonSchemaFactory.getSchema(InputStream) - branch coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "null"
        }
      ],
      "expected": "IllegalArgumentException (or NullPointerException) because the InputStream argument is null",
      "description": "Verify that the method validates its argument and fails fast when a null InputStream is supplied."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(new byte[0])"
        }
      ],
      "expected": "JsonSchemaException (or IOException) indicating that the stream does not contain a parsable JSON/YAML document",
      "description": "An empty stream cannot be parsed into a schema; the method should propagate the parsing error."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(\"{\\\"$schema\\\":\\\"http://json-schema.org/draft-07/schema#\\\",\\\"type\\\":\\\"object\\\"}\".getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "non‑null JsonSchema instance whose meta‑schema id is \"http://json-schema.org/draft-07/schema#\" and whose root node type is OBJECT",
      "description": "A minimal valid JSON Schema should be parsed successfully and a JsonSchema object returned."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(\"type: object\\n$schema: http://json-schema.org/draft-07/schema#\".getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "non‑null JsonSchema instance (YAML input is accepted) with the same properties as the JSON case above",
      "description": "The factory must detect YAML content, use the YAML mapper and still produce a correct JsonSchema."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(\"{ invalid json }\".getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "JsonProcessingException (wrapped in a RuntimeException/JsonSchemaException) because the content is not valid JSON/YAML",
      "description": "Malformed JSON/YAML should cause a parsing exception that the method propagates."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "new ByteArrayInputStream(\"{\\\"$ref\\\": \\\"classpath:/external-schema.json\\\"}\".getBytes(StandardCharsets.UTF_8))"
        }
      ],
      "expected": "non‑null JsonSchema instance whose $ref is resolved using the default ResourceLoader (i.e., external-schema.json is loaded)",
      "description": "When the schema contains a $ref, getSchema must create a ValidationContext that can resolve the reference; this case ensures the reference‑resolution branch is exercised."
    }
  ]
}