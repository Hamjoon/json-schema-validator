{
  "testCases": [
    {
      "group": "NullInputString",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "SimpleListOutputFormat"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "When the input string is null the method should fail early (deserialize will throw) and propagate an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "NullInputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": null
            },
            {
              "parameter": "format",
              "value": "SimpleListOutputFormat"
            }
          ],
          "expected": "NullPointerException",
          "description": "A null InputFormat cannot be used for deserialization; the method is expected to throw a NullPointerException."
        }
      ]
    },
    {
      "group": "ValidJsonWithDefaultOutputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":30,\"name\":\"Alice\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "SimpleListOutputFormat"
            }
          ],
          "expected": "[]",
          "description": "A well‑formed JSON that satisfies the schema should produce an empty list of ValidationMessage when using the simple list OutputFormat."
        }
      ]
    },
    {
      "group": "MalformedJsonThrowsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{invalidJson: true,,}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "SimpleListOutputFormat"
            }
          ],
          "expected": "com.fasterxml.jackson.core.JsonParseException",
          "description": "When the JSON string cannot be parsed, deserialize throws JsonParseException which should propagate out of validate."
        }
      ]
    },
    {
      "group": "CustomOutputFormatReturnsBoolean",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"valid\":true}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "BooleanOutputFormat"
            }
          ],
          "expected": "true",
          "description": "Using a custom OutputFormat that returns a Boolean should cause validate to return the value supplied by the format (true for a valid instance)."
        }
      ]
    },
    {
      "group": "EmptyJsonObject",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "SimpleListOutputFormat"
            }
          ],
          "expected": "[]",
          "description": "An empty JSON object that conforms to the schema (no required properties) should result in an empty ValidationMessage list."
        }
      ]
    },
    {
      "group": "JsonArrayInput",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "[1,2,3]"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "SimpleListOutputFormat"
            }
          ],
          "expected": "[]",
          "description": "When the schema permits an array, a valid JSON array should also produce an empty list of ValidationMessage."
        }
      ]
    }
  ]
}