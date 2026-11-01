{
  "result": [
    {
      "group": "Malformed YAML Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"name: John\\nage: 30: extra\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Throws com.fasterxml.jackson.core.JsonParseException (or a wrapped ValidationException) because the YAML input is malformed and cannot be deserialized.",
          "description": "Provides a syntactically invalid YAML document to trigger the deserialization error path for the YAML branch."
        }
      ]
    },
    {
      "group": "Invalid Percent Encoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"%E4%BD%A0%E5%A5\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Throws IllegalArgumentException (or UnsupportedEncodingException) caused by malformed percent‑encoding when URLDecoder attempts to decode the input.",
          "description": "Supplies a URL‑encoded string with an incomplete percent‑encoding sequence, exercising the branch that validates percent‑encoding before decoding."
        }
      ]
    },
    {
      "group": "Empty URL Encoded Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Throws com.fasterxml.jackson.core.JsonParseException (or a wrapped ValidationException) because after decoding the empty string the JSON parser fails.",
          "description": "Tests the behavior when an empty string is supplied with URL_ENCODED format; decoding yields an empty string which cannot be parsed as JSON/YAML."
        }
      ]
    },
    {
      "group": "Whitespace URL Encoded Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"   \\n   \""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.URL_ENCODED"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "Throws com.fasterxml.jackson.core.JsonParseException (or a wrapped ValidationException) because after decoding the whitespace‑only string cannot be parsed as JSON/YAML.",
          "description": "Ensures that whitespace‑only URL‑encoded input is not treated as valid content and triggers a parsing exception."
        }
      ]
    },
    {
      "group": "Custom Execution Context Invalid Configuration",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setMaxDepth(-1)"
            }
          ],
          "expected": "Throws IllegalArgumentException because the execution customizer attempts to set a negative max depth, which is invalid.",
          "description": "Customizes the ExecutionContext with an illegal configuration (negative max depth) to verify that the validator validates customizer‑provided settings."
        }
      ]
    }
  ]
}