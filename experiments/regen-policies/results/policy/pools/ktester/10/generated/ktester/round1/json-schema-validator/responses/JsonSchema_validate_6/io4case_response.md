{
  "testCases": [
    {
      "group": "Valid inputs - whitespace handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "   {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}   "
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "The JSON string contains leading and trailing whitespace; the method should trim/ignore whitespace, deserialize successfully and return an empty validation result set."
        }
      ]
    },
    {
      "group": "Valid inputs - Unicode characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"type\":\"object\",\"properties\":{\"emoji\":{\"type\":\"string\"}},\"description\":\"😀\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "The JSON payload contains Unicode characters (emoji) in a property value; deserialization must handle UTF‑8 correctly and validation should succeed."
        }
      ]
    },
    {
      "group": "Invalid inputs - whitespace‑only string",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "   \n\t  "
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonProcessingException (or ValidationException) thrown because the trimmed input is empty and cannot be parsed into a JsonNode.",
          "description": "A string that contains only whitespace should be treated as invalid JSON/YAML; deserialization must fail and the method should propagate the parsing exception."
        }
      ]
    },
    {
      "group": "Invalid inputs - instance violates schema (missing required property)",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":null}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a message about the required property 'age' being missing or null.",
          "description": "The JsonSchema instance is defined with a required integer property \"age\". Supplying an instance where the property is null (or absent) should produce a validation error."
        }
      ]
    },
    {
      "group": "Invalid inputs - type mismatch (string instead of integer)",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"age\":\"twenty\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a message that the value of 'age' is not of type integer.",
          "description": "The schema requires \"age\" to be an integer; providing a string should trigger a type‑mismatch validation message."
        }
      ]
    },
    {
      "group": "Invalid inputs - JSON array when object expected",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "[{\"age\":30}]"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "A Set<ValidationMessage> containing a message that the instance type is array but the schema expects an object.",
          "description": "The compiled schema expects a JSON object; supplying an array should result in a validation error about the mismatched root type."
        }
      ]
    },
    {
      "group": "Edge cases - UTF‑8 BOM prefix",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\uFEFF{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "The input string starts with a UTF‑8 Byte Order Mark; the method should ignore the BOM, deserialize correctly and return an empty validation set."
        }
      ]
    },
    {
      "group": "Special handling - JSON with comments (should fail)",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{/* comment */ \"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonProcessingException (or ValidationException) thrown because standard JSON parsers do not allow comments.",
          "description": "Even though the comment is syntactically valid in some extensions, the library used by JsonSchema does not support comments; deserialization must fail."
        }
      ]
    },
    {
      "group": "Valid inputs - YAML with Windows line endings",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "type: object\r\nproperties:\r\n  name:\r\n    type: string\r\n"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "An empty Set<ValidationMessage> (no validation errors).",
          "description": "YAML content that uses CRLF line endings should be parsed without issues; the method should return an empty validation result set."
        }
      ]
    },
    {
      "group": "Invalid inputs - malformed URL‑encoded JSON",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%7B%22type%22%3A%22object%22%7"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonProcessingException (or ValidationException) because after URL‑decoding the string is syntactically invalid JSON.",
          "description": "A URL‑encoded payload that is missing a closing brace should be decoded first and then fail during JSON deserialization."
        }
      ]
    },
    {
      "group": "Invalid inputs - both input and InputFormat null",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException (or NullPointerException) indicating that both parameters must not be null.",
          "description": "When both arguments are null the method should fail fast, reporting that required arguments are missing before any deserialization attempt."
        }
      ]
    }
  ]
}