{
  "tests": [
    {
      "group": "UTF-8 BOM handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream((new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF})\n    .concat(\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"age\\\":{\\\"type\\\":\\\"integer\\\"}}}\".getBytes(StandardCharsets.UTF_8)))"
            }
          ],
          "expected": "non‑null JsonSchema instance whose root node type is OBJECT and contains a property named \"age\"",
          "description": "The input stream starts with a UTF‑8 Byte Order Mark. The factory should skip the BOM and correctly parse the JSON schema."
        }
      ]
    },
    {
      "group": "Unsupported $schema version",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream('{\"$schema\":\"http://json-schema.org/draft-2020-12/schema#\",\"type\":\"object\"}'.getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"Unsupported $schema\"",
          "description": "The schema declares a $schema value that the library does not support (draft‑2020‑12). The method should reject it with a clear IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Large schema handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream(generateLargeSchema().getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "non‑null JsonSchema instance (parsing succeeds without OutOfMemoryError)",
          "description": "A generated schema containing 10 000 properties tests the method's ability to handle large inputs without blowing up."
        }
      ]
    },
    {
      "group": "Internal $ref resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream('{\"$ref\":\"#/definitions/name\",\"definitions\":{\"name\":{\"type\":\"string\"}}}'.getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "non‑null JsonSchema instance whose resolved type is STRING (the $ref is successfully dereferenced)",
          "description": "The schema uses an internal $ref to a definition. The factory should resolve the reference and produce a JsonSchema whose effective type is STRING."
        }
      ]
    },
    {
      "group": "Schema with $id URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "new ByteArrayInputStream('{\"$id\":\"http://example.com/schemas/person\",\"type\":\"object\",\"properties\":{\"firstName\":{\"type\":\"string\"}}}'.getBytes(StandardCharsets.UTF_8))"
            }
          ],
          "expected": "non‑null JsonSchema instance whose schema location equals URI \"http://example.com/schemas/person\"",
          "description": "When a schema defines an $id, the factory should use it as the SchemaLocation. The test verifies that the resulting JsonSchema reflects the provided URI."
        }
      ]
    }
  ],
  "helperMethods": [
    {
      "name": "generateLargeSchema",
      "code": "private static String generateLargeSchema() {\n    StringBuilder sb = new StringBuilder();\n    sb.append('{');\n    sb.append('\"type\":\"object\",');\n    sb.append('\"properties\":{');\n    for (int i = 0; i < 10000; i++) {\n        sb.append('\"prop').append(i).append('\":{\"type\":\"string\"}');\n        if (i < 9999) sb.append(',');\n    }\n    sb.append('}');\n    sb.append('}');\n    return sb.toString();\n}"
    }
  ]
}