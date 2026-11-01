{
  "groups": [
    {
      "group": "Null Input Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>> (standard list collector)"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling validate with a null JSON node should immediately throw NullPointerException regardless of the OutputFormat."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"name\":\"John\"}"
            },
            {
              "parameter": "format",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling validate with a null OutputFormat should throw NullPointerException even when the JSON node is valid."
        }
      ]
    },
    {
      "group": "Different OutputFormat Implementations",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"age\":30}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<Set<ValidationMessage>> that collects messages into a Set"
            }
          ],
          "expected": "Set containing a ValidationMessage about missing required property \"name\"",
          "description": "Using a Set‑based OutputFormat should still return the correct validation message for a missing required property."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"name\":\"John\",\"age\":\"thirty\"}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<String> that serialises messages to a JSON string"
            }
          ],
          "expected": "\"[{'message':'instance type is not integer','path':'/age'}]\"",
          "description": "When the schema expects an integer for \"age\" but receives a string, a custom String‑based OutputFormat should return a JSON‑encoded string of the ValidationMessage."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"name\":\"John\"}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<Object> that throws RuntimeException during format.apply"
            }
          ],
          "expected": "RuntimeException",
          "description": "If the OutputFormat implementation throws an exception while processing, the validate method should propagate that exception."
        }
      ]
    },
    {
      "group": "Type Validator Scenarios",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"age\":25}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>>"
            }
          ],
          "expected": "Empty list []",
          "description": "Schema defines \"age\" as integer type; providing a correct integer value should produce no validation messages."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"age\":\"25\"}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>>"
            }
          ],
          "expected": "List containing a ValidationMessage about type mismatch for property \"age\"",
          "description": "When the instance type does not match the schema’s type validator (string instead of integer), a type‑mismatch ValidationMessage must be returned."
        }
      ]
    },
    {
      "group": "$ref Resolution",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"address\":{\"city\":\"NY\",\"zip\":10001}}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>>"
            }
          ],
          "expected": "Empty list []",
          "description": "Schema contains a $ref to an external address schema; a valid address object should resolve the reference and produce no errors."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"address\":{\"city\":123,\"zip\":\"10001\"}}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>>"
            }
          ],
          "expected": "List with two ValidationMessages: one for type mismatch on \"city\", one for type mismatch on \"zip\"",
          "description": "Invalid types inside a $ref‑referenced subschema should be reported correctly after reference resolution."
        }
      ]
    },
    {
      "group": "Recursive Anchor Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing JSON {\"node\":{\"node\":{\"node\":null}}}"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>>"
            }
          ],
          "expected": "Empty list []",
          "description": "Schema uses a recursive anchor to allow nested \"node\" objects; a finite depth instance should validate without errors."
        },
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "ObjectNode representing a deeply nested structure that exceeds the recursion limit (e.g., 1000 levels of \"node\")"
            },
            {
              "parameter": "format",
              "value": "OutputFormat<List<ValidationMessage>>"
            }
          ],
          "expected": "List containing a ValidationMessage indicating recursion depth exceeded or stack overflow protection triggered",
          "description": "When the instance depth exceeds the library’s safe recursion limit, validation should stop and emit a message rather than cause a StackOverflowError."
        }
      ]
    }
  ]
}