{
  "testCases": [
    {
      "group": "Valid Input - JSON Schema String",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"
            }
          ],
          "expected": "instance of com.networknt.schema.JsonSchema",
          "description": "A well‑formed JSON schema string should be parsed and a non‑null JsonSchema object returned."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: object\nproperties:\n  age:\n    type: integer"
            }
          ],
          "expected": "instance of com.networknt.schema.JsonSchema",
          "description": "A well‑formed YAML schema string should be detected, converted to JSON and a JsonSchema object returned."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{ \"$ref\": \"http://json-schema.org/draft-07/schema#\" }"
            }
          ],
          "expected": "instance of com.networknt.schema.JsonSchema",
          "description": "A JSON schema containing a $ref to an external meta‑schema should be resolved (using the default config) and a JsonSchema object returned without throwing."
        }
      ]
    },
    {
      "group": "Invalid Input - Null or Empty",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": null
            }
          ],
          "expected": "java.lang.NullPointerException",
          "description": "Passing a null schema string should result in a NullPointerException (the method does not accept null)."
        },
        {
          "input": [
            {
              "parameter": "schema",
              "value": ""
            }
          ],
          "expected": "java.lang.IllegalArgumentException",
          "description": "An empty string does not represent a valid schema and should cause an IllegalArgumentException."
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
              "value": "{ \"type\": \"object\", \"properties\": { \"id\": { \"type\": \"string\" } "   }
            ]
          },
          "expected\": "    ,  "description"
        ]
      }
    ]
  }