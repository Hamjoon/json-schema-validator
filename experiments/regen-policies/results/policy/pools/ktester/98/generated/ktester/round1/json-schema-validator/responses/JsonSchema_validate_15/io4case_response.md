Invalid control character at: line 128 column 180 (char 6308)
{
  "test_cases": [
    {
      "group": "Empty and Whitespace Input",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"\"" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonSchemaException wrapping JsonProcessingException (empty string is not valid JSON)",
          "description": "An empty JSON string should cause deserialization to fail, resulting in a JsonSchemaException that wraps the underlying JsonProcessingException."
        },
        {
          "input": [
            { "parameter": "input", "value": "\"   \\n\\t  \"" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "JsonSchemaException wrapping JsonProcessingException (whitespace only is not valid JSON)",
          "description": "A string that contains only whitespace characters should also be rejected during deserialization, producing a JsonSchemaException."
        }
      ]
    },
    {
      "group": "Large Valid JSON Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"items\\\":[\" + \",\".repeat(10000) + \"]}\""
            },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "Empty Set (no validation messages)",
          "description": "A very large but syntactically correct JSON document should be processed without error and return an empty validation result."
        }
      ]
    },
    {
      "group": "ExecutionCustomizer Exception Propagation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"name\":\"John\"}" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new IllegalStateException(\"customizer failure\"); }"
            }
          ],
          "expected": "IllegalStateException with message \"customizer failure\"",
          "description": "If the ExecutionContextCustomizer throws a runtime exception, the validate method must let the exception propagate unchanged."
        }
      ]
    },
    {
      "group": "OutputFormat Exception Propagation",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"age\":25}" },
            { "parameter": "inputFormat", "value": "JsonInputFormat.INSTANCE" },
            {
              "parameter": "format",
              "value": "new OutputFormat<Set<ValidationMessage>>() { public Set<ValidationMessage> format(Set<ValidationMessage> messages) { throw new RuntimeException(\"format failure\"); } }"
            },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "RuntimeException with message \"format failure\"",
          "description": "When the supplied OutputFormat throws an exception while formatting the validation messages, the exception should propagate to the caller."
        }
      ]
    },
    {
      "group": "$ref Resolution and Recursive Anchor",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"person\":{\"name\":\"Alice\",\"age\":30}}"
            },
            {
              "parameter": "inputFormat",
              "value": "JsonInputFormat.INSTANCE"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat<Set<ValidationMessage>>()"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Empty Set (no validation messages)",
          "description": "Schema contains a $ref to a definition and a $recursiveAnchor. The validator must resolve the $ref and handle the recursive anchor correctly, resulting in successful validation."
        }
      ],
      "schema_setup": {
        "schemaJson": "{\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"$id\": \"http://example.com/root.json\",\n  \"$recursiveAnchor\": true,\n  \"type\": \"object\",\n  \"properties\": {\n    \"person\": { \"$ref\": \"#/definitions/person\" }\n  },\n  \"definitions\": {\n    \"person\": {\n      \"type\": \"object\",\n      \"properties\": {\n        \"name\": { \"type\": \"string\" },\n        \"age\": { \"type\": \"integer\" }\n      },\n      \"required\": [\"name\", \"age\"]\n    }\n  },\n  \"required\": [\"person\"]\n}"
      }
    },
    {
      "group": "Custom InputFormat Returning Null JsonNode",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "{\"dummy\":\"value\"}" },
            { "parameter": "inputFormat", "value": "NullReturningInputFormat.INSTANCE" },
            { "parameter": "format", "value": "new SimpleOutputFormat<Set<ValidationMessage>>()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "NullPointerException with message \"Deserialized JsonNode must not be null\"",
          "description": "A custom InputFormat that returns null instead of a JsonNode should cause the validate method to throw a NullPointerException before validation begins."
        }
      ],
      "custom_inputformat": {
        "className": "NullReturningInputFormat",
        "code": "public enum NullReturningInputFormat implements InputFormat {\n    INSTANCE;\n    @Override\n    public JsonNode deserialize(String input) {\n        return null; // deliberately return null\n    }\n}"
      }
    }
  ]
}