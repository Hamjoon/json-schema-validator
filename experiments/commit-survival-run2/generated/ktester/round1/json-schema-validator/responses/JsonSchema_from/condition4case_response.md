{
  "testCases": [
    {
      "group": "Object Creation - Standard Parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#/properties/name\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"http://example.com/schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "null"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "instance of JsonSchema with the supplied arguments stored in its fields",
          "description": "Verify that the static factory method creates a JsonSchema object when all parameters are valid and non‑null, and that the suppress flag is false."
        }
      ]
    },
    {
      "group": "Object Creation - Null Parent Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"#\""
            },
            {
              "parameter": "currentUri",
              "value": "URI.create(\"file:/tmp/schema.json\")"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "null"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "true"
            }
          ],
          "expected": "instance of JsonSchema with parent == null and suppressSubSchemaRetrieval == true",
          "description": "Ensure that the method correctly handles a null parent schema and sets the suppress flag to true without throwing an exception."
        }
      ]
    },
    {
      "group": "Object Creation - Empty Schema Path & Null URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "new ValidationContext()"
            },
            {
              "parameter": "schemaPath",
              "value": "\"\""
            },
            {
              "parameter": "currentUri",
              "value": "null"
            },
            {
              "parameter": "schemaNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "parent",
              "value": "new JsonSchema(new ValidationContext(), \"#/root\", URI.create(\"http://example.com\"), JsonNodeFactory.instance.objectNode(), null, false)"
            },
            {
              "parameter": "suppressSubSchemaRetrieval",
              "value": "false"
            }
          ],
          "expected": "instance of JsonSchema where getCurrentUri() returns null and schemaPath is an empty string",
          "description": "Test that the factory method can be called with an empty schemaPath and a null currentUri, which are legal values for certain draft specifications."
        }
      ]
    }
  ]
}