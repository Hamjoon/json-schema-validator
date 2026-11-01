{
  "testCases": [
    {
      "group": "Valid JSON schema input",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a minimal valid JSON schema (e.g., {\"type\":\"object\"})"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose meta‑schema version matches the default version and whose root node equals the parsed JSON schema",
          "description": "Verifies that a well‑formed JSON schema read from an InputStream is parsed correctly and a JsonSchema object is returned."
        }
      ]
    },
    {
      "group": "Valid YAML schema input",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a minimal valid YAML schema (e.g., \"type: object\")"
            }
          ],
          "expected": "JsonSchema instance (non‑null) whose root node equals the parsed YAML schema converted to JSON",
          "description": "Ensures that when the InputStream contains YAML, the factory detects the format, uses the YAML mapper and returns a proper JsonSchema."
        }
      ]
    },
    {
      "group": "Empty InputStream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream with zero bytes"
            }
          ],
          "expected": "IllegalArgumentException (or JsonSchemaException) indicating that the schema content is empty",
          "description": "Checks the branch that validates the content length; an empty stream should trigger an exception rather than returning a schema."
        }
      ]
    },
    {
      "group": "Null InputStream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) because the method does not accept a null stream",
          "description": "Validates the defensive check for a null argument at the beginning of the method."
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
              "value": "ByteArrayInputStream containing malformed JSON (e.g., \"{type:object\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating parsing failure",
          "description": "Tests the branch where the underlying ObjectMapper fails to parse the content; the method should propagate a parsing exception."
        },
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing malformed YAML (e.g., \"type: [object\")"
            }
          ],
          "expected": "JsonSchemaException (or IOException) indicating parsing failure",
          "description": "Same as above but for YAML detection path."
        }
      ]
    },
    {
      "group": "IOException while reading InputStream",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "InputStream mock that throws IOException on any read operation"
            }
          ],
          "expected": "IOException propagated from readTree",
          "description": "Ensures that an IOException occurring during stream reading is not swallowed and is re‑thrown to the caller."
        }
      ]
    },
    {
      "group": "Schema with $id referencing another schema (loader branch)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaStream",
              "value": "ByteArrayInputStream containing a JSON schema with an \"$id\" field pointing to another schema URI"
            }
          ],
          "expected": "JsonSchema instance whose location is resolved via SchemaLoader (non‑null) and whose internal reference map contains the referenced schema",
          "description": "Triggers the branch where the factory creates a SchemaLocation, invokes getSchemaLoader(), and resolves external references."
        }
      ]
    }
  ]
}