{
  "test_cases": [
    {
      "group": "hasTypeValidator - null JsonSchema reference",
      "cases": [
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "Calling hasTypeValidator() on a null JsonSchema reference should throw NullPointerException as per Java semantics."
        }
      ]
    },
    {
      "group": "hasTypeValidator - schema with invalid type value",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaJson",
              "value": "{ \"type\": 123 }"
            }
          ],
          "expected": "JsonSchemaException",
          "description": "The \"type\" keyword must be a string or an array. Supplying a numeric value makes the schema invalid; during schema creation the library throws JsonSchemaException, which propagates when hasTypeValidator() is invoked."
        }
      ]
    },
    {
      "group": "hasTypeValidator - circular $ref causing stack overflow",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootSchemaJson",
              "value": "{ \"$ref\": \"#/definitions/circle\", \"definitions\": { \"circle\": { \"$ref\": \"#/definitions/circle\" } } }"
            }
          ],
          "expected": "StackOverflowError",
          "description": "A $ref that resolves to itself creates an infinite recursion during schema resolution. When hasTypeValidator() triggers getTypeValidator(), the recursive resolution eventually overflows the stack."
        }
      ]
    },
    {
      "group": "hasTypeValidator - schema node is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "schemaNode",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Creating a JsonSchema with a null JsonNode for the schema and then invoking hasTypeValidator() leads to a NullPointerException inside getTypeValidator() when it attempts to read the \"type\" property."
        }
      ]
    }
  ]
}