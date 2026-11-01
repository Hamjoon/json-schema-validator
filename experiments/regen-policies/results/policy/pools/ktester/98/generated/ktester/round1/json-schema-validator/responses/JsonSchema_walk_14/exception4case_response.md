{
  "testCases": [
    {
      "group": "Null ExecutionContext – throws NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "NullPointerException is thrown",
          "description": "The method dereferences the supplied ExecutionContext when calling walkAtNodeInternal. Supplying null should cause a NullPointerException before any deserialization occurs."
        }
      ]
    },
    {
      "group": "Null InputFormat – throws NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "{\"type\":\"object\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "NullPointerException is thrown",
          "description": "deserialize expects a non‑null InputFormat to decide how to parse the string. Passing null should result in a NullPointerException."
        }
      ]
    },
    {
      "group": "Invalid YAML input – deserialize throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "type: object\nproperties:\n  name: {type: string"   ,
              "value": "type: object\nproperties:\n  name: {type: string"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "IOException (or JsonProcessingException) is thrown from deserialize",
          "description": "The YAML string is syntactically broken (missing closing brace). When InputFormat.YAML is used, deserialize should attempt to parse it and throw an IOException/JsonProcessingException."
        }
      ]
    },
    {
      "group": "Malformed URL‑encoded input – throws IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "input",
              "value": "%7B%22type%22%3A%22object%22%ZZ"   ,
              "value": "%7B%22type%22%3A%22object%22%ZZ"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException is thrown during URL decoding",
          "description": "The input contains an invalid percent‑encoding sequence (%ZZ). URLDecoder.decode should detect the malformed escape and throw IllegalArgumentException before deserialization."
        }
      ]
    }
  ]
}