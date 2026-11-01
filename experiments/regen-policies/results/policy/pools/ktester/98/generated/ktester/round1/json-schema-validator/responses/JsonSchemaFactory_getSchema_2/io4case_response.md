{
  "testCases": [
    {
      "group": "UTF-8 BOM handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing UTF‑8 BOM (0xEF,0xBB,0xBF) followed by a minimal valid JSON schema: {\"type\":\"object\"}"
            }
          ],
          "expected": "JsonSchema instance (non‑null) – parsing succeeds despite the leading BOM and the returned schema’s root node equals the parsed JSON object.",
          "description": "Verifies that the factory correctly strips a UTF‑8 byte‑order‑mark before parsing the JSON schema."
        }
      ]
    },
    {
      "group": "Leading whitespace and newline handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing whitespace and newline characters before a valid JSON schema, e.g., \"\\n  \\t  {\\\"type\\\":\\\"object\\\"}\""
            }
          ],
          "expected": "JsonSchema instance (non‑null) – the whitespace is ignored and the schema is parsed correctly.",
          "description": "Ensures that leading whitespace and line breaks do not interfere with format detection or parsing."
        }
      ]
    },
    {
      "group": "$schema version detection (draft‑07)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a JSON schema with an explicit $schema URI for Draft‑07: {\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose meta‑schema version is Draft‑07 (SpecVersion.VersionFlag.V7).",
          "description": "Checks that the factory respects the $schema keyword and selects the appropriate meta‑schema version."
        }
      ]
    },
    {
      "group": "Unsupported $schema URI fallback",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a JSON schema with an unknown $schema URI: {\"$schema\":\"http://example.com/unknown/schema#\",\"type\":\"object\"}"
            }
          ],
          "expected": "JsonSchema instance (non‑null) that falls back to the factory’s default meta‑schema version (e.g., Draft‑2019‑09) without throwing an exception.",
          "description": "Validates that an unrecognised $schema URI does not cause a failure and that the default meta‑schema is used."
        }
      ]
    },
    {
      "group": "Unicode characters in schema property names",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a JSON schema with a Unicode property name: {\"type\":\"object\",\"properties\":{\"café\":{\"type\":\"string\"}}}"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose parsed root node contains the property name \"café\" exactly as supplied.",
          "description": "Ensures proper UTF‑8 handling for non‑ASCII characters inside the schema."
        }
      ]
    },
    {
      "group": "Internal $ref handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a JSON schema with an internal $ref: {\"$defs\":{\"addr\":{\"type\":\"object\",\"properties\":{\"street\":{\"type\":\"string\"}}}},\"type\":\"object\",\"properties\":{\"address\":{\"$ref\":\"#/$defs/addr\"}}}"
            }
          ],
          "expected": "JsonSchema instance (non‑null) where the internal reference is resolved – the resulting schema’s \"address\" property behaves as an object with a \"street\" string property.",
          "description": "Triggers the branch that resolves $ref pointers within the same document."
        }
      ]
    },
    {
      "group": "Duplicate keys handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a JSON schema with duplicate keys: {\"type\":\"object\",\"type\":\"string\"}"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose root node’s \"type\" field equals \"string\" (the last occurrence wins).",
          "description": "Confirms that the underlying Jackson mapper’s default duplicate‑key handling (last‑value wins) does not cause an exception."
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
              "value": "ByteArrayInputStream containing a generated JSON schema with 1,000 properties (e.g., {\"type\":\"object\",\"properties\":{\"p0\":{\"type\":\"string\"},...,\"p999\":{\"type\":\"string\"}}})"
            }
          ],
          "expected": "JsonSchema instance (non‑null) – the factory successfully reads and parses a large schema without running out of memory or throwing an exception.",
          "description": "Tests the method’s ability to handle schemas of considerable size, ensuring stream reading and parsing scale."
        }
      ]
    }
  ]
}