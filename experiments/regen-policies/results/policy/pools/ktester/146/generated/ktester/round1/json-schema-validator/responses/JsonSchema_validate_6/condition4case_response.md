{
  "testCases": [
    {
      "group": "Valid JSON Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\",\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "A well‑formed JSON string is supplied with the JSON input format. The method should deserialize the string successfully and return an empty validation result because the instance complies with the schema."
        }
      ]
    },
    {
      "group": "Valid YAML Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: John\nage: 30"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "A well‑formed YAML document is supplied with the YAML input format. The method should correctly deserialize the YAML and return no validation messages."
        }
      ]
    },
    {
      "group": "Invalid JSON Syntax",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\", \"age\":}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "A Set<ValidationMessage> containing at least one message that indicates a parsing error (e.g., \"unexpected token\" or \"cannot deserialize\").",
          "description": "The JSON string is syntactically malformed. Deserialization should fail and the validator must return a validation message describing the parse error."
        }
      ]
    },
    {
      "group": "Invalid YAML Syntax",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: John\nage: : 30"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a parsing‑error message for YAML.",
          "description": "The YAML document contains an illegal token. The method should catch the parsing exception and surface it as a validation message."
        }
      ]
    },
    {
      "group": "Null Input String",
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
          "expected": "IllegalArgumentException with message \"Input string cannot be null\" (or similar).",
          "description": "Passing a null input string should be rejected early, before any deserialization attempt, resulting in an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Empty Input String",
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
          "expected": "A Set<ValidationMessage> containing a parsing error indicating empty content.",
          "description": "An empty string cannot be parsed into a JSON node; the method should return a validation message that reflects the inability to deserialize an empty payload."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.UNKNOWN"
            }
          ],
          "expected": "UnsupportedOperationException (or IllegalArgumentException) stating that the provided InputFormat is not supported.",
          "description": "When an input format that the validator does not recognise is supplied, the method should throw an exception rather than attempting deserialization."
        }
      ]
    },
    {
      "group": "Input with URL‑Encoded Characters (UTF‑8)",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%7B%22name%22%3A%22John%22%7D"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED_JSON"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors) after successful URL decoding and JSON deserialization.",
          "description": "When the input format indicates URL‑encoded JSON, the method should first decode the string (using UTF‑8) and then validate the resulting JSON. No validation errors are expected for a correct payload."
        }
      ]
    },
    {
      "group": "Input with Unsupported Character Encoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%E4%BD%A0%E5%A5%BD"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED_JSON"
            }
          ],
          "expected": "UnsupportedEncodingException wrapped in a ValidationException or propagated as an IllegalStateException.",
          "description": "If URL decoding fails because the underlying charset is not supported, the method should surface the error appropriately (commonly as an unchecked exception)."
        }
      ]
    }
  ]
}