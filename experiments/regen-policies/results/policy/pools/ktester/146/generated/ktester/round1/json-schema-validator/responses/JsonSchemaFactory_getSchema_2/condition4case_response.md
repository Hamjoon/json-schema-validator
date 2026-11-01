{
  "tests": [
    {
      "group": "Valid JSON schema parsing",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream('{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}'.getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "non‑null JsonSchema instance whose root node type is OBJECT and contains a property named \"name\"",
          "description": "A well‑formed JSON schema is supplied. The method should parse the stream, create a JsonSchema object and return it without throwing any exception."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream('{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"array\"}'.getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "non‑null JsonSchema instance whose meta‑schema version is DRAFT_07",
          "description": "The schema contains an explicit $schema keyword. The factory should recognise the version flag, load the correct meta‑schema and return a JsonSchema that is aware of the Draft‑07 rules."
        }
      ]
    },
    {
      "group": "Valid YAML schema parsing",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(\"type: object\\nproperties:\\n  age:\\n    type: integer\".getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "non‑null JsonSchema instance representing the same structure as the JSON equivalent",
          "description": "When the input stream contains a valid YAML schema, the factory should automatically select the YAML mapper, convert it to a JsonNode and return a JsonSchema instance."
        }
      ]
    },
    {
      "group": "Empty or whitespace only input",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(\"   \".getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"Schema content is empty\"",
          "description": "An input stream that only contains whitespace should be treated as an empty schema and cause the method to reject it with a clear exception."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(new byte[0])"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"Schema content is empty\"",
          "description": "An empty stream (zero bytes) must also trigger the same validation path as the whitespace‑only case."
        }
      ]
    },
    {
      "group": "Invalid JSON/YAML content",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream('{invalid json}'.getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "IOException (or JsonProcessingException) indicating parsing failure",
          "description": "Malformed JSON should cause the underlying JsonNodeReader to throw an IOException, which propagates out of getSchema."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream('type: [unbalanced'.getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "IOException (or JsonProcessingException) indicating parsing failure",
          "description": "Malformed YAML should also result in a parsing exception."
        }
      ]
    },
    {
      "group": "Null input stream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "null"
            }
          ],
          "expected": "NullPointerException with message containing \"schemaStream\"",
          "description": "Passing a null InputStream must be guarded against; the method should throw NPE before attempting any processing."
        }
      ]
    },
    {
      "group": "IO error while reading stream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new InputStream() { @Override public int read() throws IOException { throw new IOException(\"simulated read error\"); } }"
            }
          ],
          "expected": "IOException with message \"simulated read error\"",
          "description": "If the underlying InputStream throws an IOException during read, the method should propagate the exception unchanged."
        }
      ]
    }
  ]
}