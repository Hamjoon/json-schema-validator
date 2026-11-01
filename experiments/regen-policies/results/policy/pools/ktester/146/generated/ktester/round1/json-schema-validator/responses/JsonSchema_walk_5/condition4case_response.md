{
  "testCases": [
    {
      "group": "Valid Input - JSON format, validation enabled",
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
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "A well‑formed JSON document is supplied. The walk method should deserialize the string, create an ExecutionContext and perform validation, returning a successful ValidationResult."
        }
      ]
    },
    {
      "group": "Valid Input - JSON format, validation disabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "{\"city\":\"Paris\",\"population\":2148000}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": false
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "Even when validation is turned off, walk should still return a successful ValidationResult because deserialization succeeds."
        }
      ]
    },
    {
      "group": "Invalid JSON syntax",
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
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == false and ValidationResult.getMessages().size() > 0",
          "description": "The JSON string is malformed (trailing comma). deserialize should throw a parsing exception which is captured and reported in the ValidationResult."
        }
      ]
    },
    {
      "group": "Null input string",
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
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "throws IllegalArgumentException",
          "description": "walk should reject a null input string early (either in deserialize or in the public overload) and raise an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Unsupported InputFormat (e.g., YAML when only JSON is supported)",
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
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "throws UnsupportedOperationException or IOException",
          "description": "When an InputFormat that the current JsonSchema implementation cannot handle is supplied, deserialize should fail and the walk method should propagate the exception."
        }
      ]
    },
    {
      "group": "URL‑encoded JSON string",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "%7B%22id%22%3A123%2C%22valid%22%3Atrue%7D"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "ValidationResult.isValid() == true and ValidationResult.getMessages().isEmpty()",
          "description": "walk should internally URL‑decode the string (via URLDecoder) before deserialization, resulting in a successful validation."
        }
      ]
    }
  ]
}