{
  "groups": [
    {
      "group": "validate(JsonNode) - malformed schema exceptions",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"valid\""
            },
            {
              "parameter": "schema",
              "value": "[ { \"type\": \"string\" } ]"
            }
          ],
          "expected": "JsonSchemaException indicating that the schema root must be an object",
          "description": "The schema node is an array instead of an object; the validator should throw a JsonSchemaException during schema parsing."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "123"
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": 123 }"
            }
          ],
          "expected": "JsonSchemaException stating that the value of \"type\" must be a string or an array of strings",
          "description": "The \"type\" keyword is given a numeric value, which is invalid according to the JSON Schema spec."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"anything\""
            },
            {
              "parameter": "schema",
              "value": "{ \"$ref\": 123 }"
            }
          ],
          "expected": "JsonSchemaException indicating that $ref value must be a string",
          "description": "A $ref keyword with a non‑string value should cause schema validation to fail with a JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"test\""
            },
            {
              "parameter": "schema",
              "value": "{ \"unknownKeyword\": true }"
            }
          ],
          "expected": "JsonSchemaException mentioning unrecognized keyword \"unknownKeyword\"",
          "description": "The schema contains a keyword that the validator does not recognize, which must be reported as a schema‑definition error."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"circular\""
            },
            {
              "parameter": "schema",
              "value": "{ \"$id\": \"#root\", \"$ref\": \"#root\" }"
            }
          ],
          "expected": "JsonSchemaException (or StackOverflowError) indicating a circular $ref without a recursive anchor",
          "description": "A schema that references itself directly without a $recursiveAnchor creates an infinite loop; the implementation should detect this and throw a JsonSchemaException rather than crashing."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"bad\""
            },
            {
              "parameter": "schema",
              "value": "{ \"type\": [\"string\", 123] }"
            }
          ],
          "expected": "JsonSchemaException stating that all entries in a type array must be strings",
          "description": "When \"type\" is an array, each element must be a valid type name; mixing a number should cause a schema‑parsing exception."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - execution‑customizer induced exceptions",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"any\""
            },
            {
              "parameter": "schema",
              "value": "{}"
            },
            {
              "parameter": "executionCustomizer",
              "value": "consumer -> { throw new RuntimeException(\"customizer failure\"); }"
            }
          ],
          "expected": "RuntimeException with message \"customizer failure\"",
          "description": "If the ExecutionContextCustomizer supplied to the overloaded validate method throws an exception, the validate call should propagate that exception."
        }
      ]
    },
    {
      "group": "validate(JsonNode) - input node causing internal I/O error",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "\"trigger\""
            },
            {
              "parameter": "schema",
              "value": "{ \"format\": \"uri\" }"
            }
          ],
          "expected": "ValidationMessage indicating format validation failure (no exception thrown)",
          "description": "The \"format\" keyword may internally attempt to parse the string as a URI; an invalid URI should not raise an exception but produce a validation message. This case confirms that no unexpected IOException is thrown."
        }
      ]
    }
  ]
}