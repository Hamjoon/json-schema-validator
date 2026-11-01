{
  "testCases": [
    {
      "group": "Valid Schemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"type\": \"string\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose meta‑schema is the default (Draft‑07). The schema should validate a JSON string successfully.",
          "description": "Pass a minimal valid JSON Schema (type string) without an explicit $schema. The factory should create a JsonSchema using the default meta‑schema (Draft‑07) and return a usable JsonSchema object."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"type\": \"integer\" }"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose meta‑schema URI is \"http://json-schema.org/draft-04/schema#\" (Draft‑04). The schema should accept integer values and reject non‑integers.",
          "description": "Provide a schema that explicitly declares Draft‑04 via $schema. The factory must recognise the $schema keyword, load the corresponding meta‑schema and return a JsonSchema bound to Draft‑04."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$id\": \"http://example.com/person\", \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
            }
          ],
          "expected": "A non‑null JsonSchema instance whose meta‑schema is the default (Draft‑07) and whose schema location is set to the provided $id URI.",
          "description": "Supply a schema that contains an $id field. The factory should keep the $id as the schema location while still using the default meta‑schema because no $schema keyword is present."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{}"
            }
          ],
          "expected": "A non‑null JsonSchema instance representing an empty schema (always valid). The meta‑schema should be the default (Draft‑07).",
          "description": "Pass an empty JSON object. The factory must still return a valid JsonSchema object that imposes no constraints."
        }
      ]
    },
    {
      "group": "Invalid Inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown because the jsonNode argument is null.",
          "description": "Calling getSchema(null) should fail fast, as the method expects a non‑null JsonNode to build a schema."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "[ 1, 2, 3 ]"
            }
          ],
          "expected": "JsonSchemaException (or similar) indicating that the root node must be an object, not an array.",
          "description": "Supply a JSON array as the schema root. The factory should detect the invalid root type and raise an appropriate exception."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://unknown-schema.org/unknown#\", \"type\": \"string\" }"
            }
          ],
          "expected": "JsonSchemaException (or fallback to default) because the $schema URI cannot be resolved to a known meta‑schema.",
          "description": "Provide a $schema value that does not correspond to any built‑in meta‑schema. The factory should either throw an exception or fall back to the default meta‑schema; the test asserts the expected failure mode."
        }
      ]
    }
  ]
}