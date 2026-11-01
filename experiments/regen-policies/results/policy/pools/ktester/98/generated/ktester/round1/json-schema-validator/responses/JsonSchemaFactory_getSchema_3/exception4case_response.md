{
  "test_cases": [
    {
      "group": "Invalid $id URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$id\": \"ht!tp://[invalid]\", \"type\": \"object\" }"
            }
          ],
          "expected": "IllegalArgumentException (or JsonSchemaException) indicating that the $id value is not a valid URI and cannot be parsed.",
          "description": "Pass a schema with a malformed $id URI. The factory should detect the invalid URI during schema location creation and throw an appropriate exception."
        }
      ]
    },
    {
      "group": "Unresolvable External $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$ref\": \"http://example.com/nonexistent-schema.json\" }"
            }
          ],
          "expected": "JsonSchemaException (or SchemaNotFoundException) because the external schema referenced by $ref cannot be loaded.",
          "description": "Supply a $ref that points to an external URI that is not available in the classpath or reachable. Schema loading should fail and raise an exception."
        }
      ]
    },
    {
      "group": "Circular $ref Detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$id\": \"http://example.com/circular\", \"$defs\": { \"A\": { \"$ref\": \"#/$defs/B\" }, \"B\": { \"$ref\": \"#/$defs/A\" } }, \"$ref\": \"#/$defs/A\" }"
            }
          ],
          "expected": "JsonSchemaException (or IllegalStateException) indicating a circular reference was detected while resolving $ref.",
          "description": "Create a schema where definitions reference each other in a loop. The factory must detect the circular reference during schema construction and throw an exception."
        }
      ]
    },
    {
      "group": "Malformed $schema Value",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"not a uri\", \"type\": \"string\" }"
            }
          ],
          "expected": "IllegalArgumentException (or JsonSchemaException) because the $schema value is not a syntactically valid URI.",
          "description": "Provide a $schema keyword with a string that is not a URI. The factory should reject the value and fail fast."
        }
      ]
    },
    {
      "group": "$ref to Non‑Schema Node",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"title\": \"MySchema\", \"$ref\": \"#/title\" }"
            }
          ],
          "expected": "JsonSchemaException (or IllegalArgumentException) indicating that the $ref target is not a schema node.",
          "description": "Reference a JSON pointer that resolves to a primitive value (the title string) rather than a schema object. The factory should raise an exception because $ref must point to a schema."
        }
      ]
    }
  ]
}