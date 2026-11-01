{
  "newTests": [
    {
      "group": "Mismatched InputFormat – JSON string with YAML InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "JsonProcessingException wrapped in RuntimeException",
          "description": "A well‑formed JSON document is supplied but the caller incorrectly specifies InputFormat.YAML. The deserializer should attempt YAML parsing and fail, resulting in a JsonProcessingException (wrapped as a RuntimeException)."
        }
      ]
    },
    {
      "group": "Mismatched InputFormat – YAML string with JSON InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "name: John"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "JsonProcessingException wrapped in RuntimeException",
          "description": "A valid YAML document is supplied while the method is told to treat it as JSON. The JSON parser will reject the YAML syntax and throw a JsonProcessingException (wrapped)."
        }
      ]
    },
    {
      "group": "Valid URL‑encoded JSON input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%7B%22name%22%3A%22John%22%7D"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "String containing no validation errors (e.g., \"[]\")",
          "description": "The input string is URL‑encoded JSON. The method should decode it via URLDecoder, deserialize the resulting JSON, validate successfully and return an empty‑error result."
        }
      ]
    },
    {
      "group": "Valid UTF‑8 characters in JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"greeting\":\"こんにちは\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "String containing no validation errors (e.g., \"[]\")",
          "description": "JSON containing Unicode characters should be correctly handled using UTF‑8 (the default charset). Validation must succeed and the output format should return an empty error list."
        }
      ]
    },
    {
      "group": "Whitespace‑only input string",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "   "
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "JsonProcessingException wrapped in RuntimeException",
          "description": "A string that contains only whitespace cannot be parsed into a JsonNode; the JSON parser will throw a JsonProcessingException which the method propagates (wrapped)."
        }
      ]
    },
    {
      "group": "Schema type mismatch – expecting string, got number",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":123}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "String containing a validation error about type mismatch for property \"age\"",
          "description": "The schema defines property \"age\" as type string. Supplying a numeric value should trigger a type‑validator error that the SimpleOutputFormat will render."
        }
      ]
    },
    {
      "group": "Schema enum violation",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"blue\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "String containing a validation error about value not being one of the allowed enum values",
          "description": "The schema restricts the root value to the enum [\"red\",\"green\"]. Providing \"blue\" must produce an enum‑validation error."
        }
      ]
    },
    {
      "group": "Schema pattern violation (invalid email)",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"email\":\"not-an-email\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "String containing a validation error about pattern mismatch for property \"email\"",
          "description": "The schema defines a regex pattern for an email address. Supplying a non‑matching string should cause a pattern‑validation error."
        }
      ]
    },
    {
      "group": "Additional properties not allowed",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"allowed\":\"yes\",\"extra\":\"no\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new SimpleOutputFormat()"
            }
          ],
          "expected": "String containing a validation error about disallowed additional property \"extra\"",
          "description": "The schema sets \"additionalProperties\": false. The presence of an unexpected property should be reported as a validation error."
        }
      ]
    },
    {
      "group": "Custom OutputFormat that throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new ThrowingOutputFormat()"
            }
          ],
          "expected": "RuntimeException thrown by OutputFormat",
          "description": "The supplied OutputFormat deliberately throws a RuntimeException when its format method is called. The validate method should let this exception propagate unchanged."
        }
      ]
    }
  ]
}