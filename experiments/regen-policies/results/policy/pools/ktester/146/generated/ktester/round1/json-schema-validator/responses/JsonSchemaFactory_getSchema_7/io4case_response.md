{
  "group": "JsonSchemaFactory.getSchema(InputStream, SchemaValidatorsConfig) – additional cases",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "validJsonSchemaStream (contains a moderately complex schema, e.g. {\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"},\"name\":{\"type\":\"string\"}}})"
        },
        {
          "parameter": "config",
          "value": "SchemaValidatorsConfig with failFast=true and typeLoose=false"
        }
      ],
      "expected": "returns a non‑null JsonSchema instance whose internal SchemaValidatorsConfig reflects failFast=true and typeLoose=false (the provided config is used, not a default one)",
      "description": "Ensures that a non‑null, customized configuration object is propagated to the created JsonSchema and that the settings are honoured during validation."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "InputStream that throws IOException on first read (e.g. new InputStream() { public int read() throws IOException { throw new IOException(\"forced\"); } })"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "throws IOException (or JsonSchemaException wrapping IOException) because the underlying readTree call cannot read the stream",
      "description": "Verifies that I/O errors while reading the schema are propagated to the caller rather than being swallowed."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "validJsonSchemaStream prefixed with UTF‑8 BOM (bytes: 0xEF,0xBB,0xBF followed by {\"type\":\"object\"})"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "returns a non‑null JsonSchema instance; parsing succeeds despite the BOM",
      "description": "Checks that a JSON schema stream containing a UTF‑8 Byte Order Mark is accepted, as Jackson's parser should ignore the BOM."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "validJsonSchemaStream with leading/trailing whitespace and newlines (e.g. \"\\n  { \\\"type\\\": \\\"object\\\" } \\n\\n\")"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "returns a non‑null JsonSchema instance; whitespace does not affect parsing",
      "description": "Ensures that the method correctly trims or ignores surrounding whitespace when parsing the JSON schema."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "validJsonSchemaStream followed by garbage characters (e.g. {\"type\":\"object\"}xyz)"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "throws com.fasterxml.jackson.core.JsonParseException (wrapped in JsonSchemaException) because extra non‑JSON content after the valid object causes a parsing error",
      "description": "Validates that the parser fails when the input contains trailing non‑JSON data, confirming strict JSON parsing."
    },
    {
      "input": [
        {
          "parameter": "schemaStream",
          "value": "validJsonSchemaStream encoded in UTF‑16LE with BOM (bytes: 0xFF,0xFE followed by UTF‑16LE representation of {\"type\":\"object\"})"
        },
        {
          "parameter": "config",
          "value": "new SchemaValidatorsConfig()"
        }
      ],
      "expected": "throws com.fasterxml.jackson.core.JsonParseException (or JsonSchemaException) because Jackson expects UTF‑8 by default and cannot parse UTF‑16 encoded content",
      "description": "Confirms that schemas encoded in an unsupported character set are rejected, leading to a parsing exception."
    }
  ]
}