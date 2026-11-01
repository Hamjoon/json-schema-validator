{
  "testCases": [
    {
      "group": "Valid inputs - JSON format",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "A well‑formed JSON schema string is supplied with InputFormat.JSON. The method should deserialize the JSON successfully and return an empty validation result set."
        },
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}},\"required\":[\"age\"]}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a single message about the missing required property when the schema is applied to an empty instance (the method itself only validates the schema, so the set should still be empty).",
          "description": "Ensures that a schema containing a required field does not trigger validation errors at schema‑parsing time; the method should still return an empty set."
        }
      ]
    },
    {
      "group": "Valid inputs - YAML format",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "type: object\nproperties:\n  name:\n    type: string"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "A well‑formed YAML schema string is supplied with InputFormat.YAML. The method should deserialize the YAML to a JsonNode and validate without errors."
        }
      ]
    },
    {
      "group": "Invalid inputs - malformed JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonProcessingException wrapped in a RuntimeException (or ValidationException) thrown by deserialize.",
          "description": "The JSON string is syntactically incorrect; deserialize should fail and the validate method must propagate the parsing exception."
        }
      ]
    },
    {
      "group": "Invalid inputs - malformed YAML",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "type: object\nproperties:\n  name:\n    type: string\n  - invalid"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "JsonProcessingException (or similar) thrown during YAML deserialization.",
          "description": "The YAML string contains a syntax error; the method should raise an exception during deserialization."
        }
      ]
    },
    {
      "group": "Edge cases - null input string",
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
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) indicating that input cannot be null.",
          "description": "Passing a null JSON string should be rejected before deserialization."
        }
      ]
    },
    {
      "group": "Edge cases - empty input string",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonProcessingException (or ValidationException) because an empty string cannot be parsed into a JsonNode.",
          "description": "An empty string is not a valid JSON/YAML document; deserialization must fail."
        }
      ]
    },
    {
      "group": "Edge cases - null InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException indicating that inputFormat must not be null.",
          "description": "The method should validate that the InputFormat argument is provided before attempting deserialization."
        }
      ]
    },
    {
      "group": "Edge cases - unsupported InputFormat value",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.UNKNOWN"
            }
          ],
          "expected": "UnsupportedOperationException (or similar) because the format is not recognised by deserialize.",
          "description": "When an enum value that the method does not handle is supplied, it should raise an appropriate exception."
        }
      ]
    },
    {
      "group": "Special handling - URL encoded input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%7B%22type%22%3A%22object%22%7D"  },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (the method should URL‑decode the string before deserialization and succeed).",
          "description": "The deserialize method uses URLDecoder; a URL‑encoded JSON payload should be decoded and validated without errors."
        }
      ]
    }
  ]
}