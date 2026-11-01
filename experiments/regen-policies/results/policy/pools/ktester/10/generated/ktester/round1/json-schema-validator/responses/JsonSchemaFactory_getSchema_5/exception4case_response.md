{
  "testCases": [
    {
      "group": "Exception - Null Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": null
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "Calling getSchema(null) should immediately throw NullPointerException because the method does not accept a null argument."
        }
      ]
    },
    {
      "group": "Exception - Empty String Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": ""
            }
          ],
          "expected": "java.lang.IllegalArgumentException",
          "description": "An empty string does not represent a schema; the method should validate the argument and throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Exception - Whitespace Only Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "   \t\n  "
            }
          ],
          "expected": "java.lang.IllegalArgumentException",
          "description": "A string that contains only whitespace is not a valid schema; the method should trim the input and, finding it empty, throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Exception - Malformed JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" }"
            }
          ],
          "expected": "com.fasterxml.jackson.core.JsonParseException",
          "description": "The JSON string is syntactically invalid (missing closing braces). Parsing should fail and the underlying Jackson parser should throw JsonParseException."
        }
      ]
    },
    {
      "group": "Exception - Malformed YAML",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: object\nproperties:\n  age: \n    type: integer: extra"
            }
          ],
          "expected": "com.fasterxml.jackson.core.JsonParseException",
          "description": "The YAML string contains illegal syntax (extra colon). Detection as YAML should lead to a parsing attempt that throws JsonParseException."
        }
      ]
    },
    {
      "group": "Exception - Invalid $ref Syntax",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"$ref\": \"not-a-valid-uri\" }"
            }
          ],
          "expected": "com.networknt.schema.JsonSchemaException",
          "description": "A $ref that is not a valid URI should cause the factory to raise a JsonSchemaException during reference resolution."
        }
      ]
    },
    {
      "group": "Exception - Unresolvable External URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"$ref\": \"http://nonexistent.example.com/schema.json\" }"
            }
          ],
          "expected": "java.io.IOException",
          "description": "When the schema references an external URI that cannot be fetched, the loader should propagate an IOException."
        }
      ]
    },
    {
      "group": "Exception - Unsupported SpecVersion Flag in Meta‑Schema Id",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-99/schema#\" }"
            }
          ],
          "expected": "java.lang.IllegalArgumentException",
          "description": "A $schema identifier that refers to an unknown draft version should cause the factory to reject the schema and throw IllegalArgumentException."
        }
      ]
    }
  ]
}