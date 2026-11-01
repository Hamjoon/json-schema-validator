{
  "testCases": [
    {
      "group": "Valid inputs - JSON format",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonSchema instance representing the supplied JSON schema",
          "description": "A well‑formed JSON schema string with InputFormat.JSON should be parsed successfully and a JsonSchema object returned."
        }
      ]
    },
    {
      "group": "Valid inputs - YAML format",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "type: object\nproperties:\n  age:\n    type: integer"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "JsonSchema instance representing the supplied YAML schema",
          "description": "A well‑formed YAML schema string with InputFormat.YAML should be parsed successfully and a JsonSchema object returned."
        }
      ]
    },
    {
      "group": "Edge case - Whitespace handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "   {\"type\":\"string\"}   "
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonSchema instance (the whitespace is ignored)",
          "description": "Schema strings containing leading/trailing whitespace must still be parsed correctly."
        }
      ]
    },
    {
      "group": "Invalid inputs - Null schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"schema must not be null\"",
          "description": "Passing a null schema string should cause the method to reject the call early."
        }
      ]
    },
    {
      "group": "Invalid inputs - Empty schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"schema must not be empty\"",
          "description": "An empty schema string is not a valid JSON/YAML document and must trigger an exception."
        }
      ]
    },
    {
      "group": "Invalid inputs - Null InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\"type\":\"number\"}"
            },
            {
              "parameter": "inputFormat",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException with message containing \"inputFormat must not be null\"",
          "description": "When the InputFormat argument is null the method should fail fast rather than guessing a format."
        }
      ]
    },
    {
      "group": "Invalid inputs - Unsupported InputFormat",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "<schema></schema>"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.XML"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"Unsupported InputFormat\"",
          "description": "The factory only supports JSON and YAML; providing XML (or any unknown enum constant) must raise an exception."
        }
      ]
    },
    {
      "group": "Invalid inputs - Mismatched format (JSON string with YAML format)",
      "cases": [
        {
          "input": [
            {
              "parameter": "schema",
              "value": "{\"type\":\"boolean\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "IOException (or JsonProcessingException) wrapped in a RuntimeException indicating parsing failure",
          "description": "Supplying a JSON document while requesting YAML parsing should cause a parsing error."
        }
      ]
    }
  ]
}