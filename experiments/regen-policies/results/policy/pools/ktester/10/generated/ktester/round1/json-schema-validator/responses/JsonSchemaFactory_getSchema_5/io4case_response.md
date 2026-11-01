{
  "testCases": [
    {
      "group": "Valid Input - Edge Cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "   { \"type\": \"string\" }   "
            }
          ],
          "expected": "instance of com.networknt.schema.JsonSchema",
          "description": "A JSON schema string with leading and trailing whitespace should be trimmed and parsed successfully, returning a JsonSchema instance."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "   "
            }
          ],
          "expected": "java.lang.IllegalArgumentException",
          "description": "A schema string that contains only whitespace is not a valid schema and should cause an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{}"
            }
          ],
          "expected": "instance of com.networknt.schema.JsonSchema",
          "description": "An empty JSON object is a valid schema (equivalent to \"any type\"); the factory should return a non‑null JsonSchema instance."
        }
      ]
    },
    {
      "group": "Invalid Input - Malformed Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"string\" }"
            }
          ],
          "expected": "com.networknt.schema.JsonSchemaException",
          "description": "A JSON string with a missing closing brace is malformed; parsing should fail and the factory should wrap the Jackson parsing error in a JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: object\nproperties:\n  name\n    type: string"
            }
          ],
          "expected": "com.networknt.schema.JsonSchemaException",
          "description": "A malformed YAML schema (incorrect indentation) should cause a parsing failure resulting in a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Valid Input - Internal $ref Resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\n  \"$id\": \"http://example.com/root.json\",\n  \"definitions\": {\n    \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 0 }\n  },\n  \"type\": \"object\",\n  \"properties\": {\n    \"age\": { \"$ref\": \"#\\/definitions\\/positiveInteger\" }\n  }\n}"
            }
          ],
          "expected": "instance of com.networknt.schema.JsonSchema",
          "description": "A schema that uses an internal $ref to a definition should be resolved correctly and produce a JsonSchema instance."
        }
      ]
    },
    {
      "group": "Invalid Input - Unresolvable External $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }"
            }
          ],
          "expected": "com.networknt.schema.JsonSchemaException",
          "description": "A $ref that points to a non‑existent external URI cannot be resolved; the factory should throw a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Invalid Input - Unknown Meta‑Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-999/schema#\", \"type\": \"object\" }"
            }
          ],
          "expected": "java.lang.IllegalArgumentException",
          "description": "If the $schema attribute references an unknown or unsupported meta‑schema version, the factory should reject it with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Invalid Input - Circular $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\n  \"$id\": \"http://example.com/circular.json\",\n  \"$ref\": \"http://example.com/circular.json\"\n}"
            }
          ],
          "expected": "com.networknt.schema.JsonSchemaException",
          "description": "A schema that directly references itself creates a circular $ref; the factory should detect the recursion and throw a JsonSchemaException rather than entering an infinite loop."
        }
      ]
    }
  ]
}