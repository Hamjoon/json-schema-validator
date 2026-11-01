{
  "newTestCases": [
    {
      "group": "jsonNodeReader is null – ObjectMapper error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "name: John\nage: thirty"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            }
          ],
          "expected": "IOException",
          "description": "When jsonNodeReader is null and the YAML content is malformed, the underlying YAML ObjectMapper throws an IOException which must be propagated."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "IOException",
          "description": "An empty string is not valid JSON; ObjectMapper.readTree throws an IOException which should be propagated."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null content string to ObjectMapper.readTree results in a NullPointerException (or IllegalArgumentException) that must be propagated."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "   "
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "IOException",
          "description": "A string containing only whitespace is not valid JSON; the ObjectMapper throws an IOException which should be propagated."
        }
      ]
    },
    {
      "group": "jsonNodeReader is null – parsing of arrays and nested structures",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "[{\"id\":1},{\"id\":2}]"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "JsonNode array with two elements each having field 'id' with values 1 and 2",
          "description": "When jsonNodeReader is null, a valid JSON array should be parsed into a JsonNode representing an array with the expected elements."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "- apple\n- banana\n- cherry"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            }
          ],
          "expected": "JsonNode array with elements ['apple','banana','cherry']",
          "description": "When jsonNodeReader is null, a valid YAML list should be parsed into a JsonNode array containing the listed string values."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "{\"person\":{\"name\":\"Alice\",\"address\":{\"city\":\"Wonderland\"}}}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "JsonNode with nested object 'person' containing 'name'='Alice' and 'address.city'='Wonderland'",
          "description": "Ensures that nested JSON objects are correctly parsed when jsonNodeReader is null."
        }
      ]
    },
    {
      "group": "jsonNodeReader is non‑null – format propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "{\"custom\":\"json\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "JsonNode returned by custom JsonNodeReader after receiving InputFormat.JSON",
          "description": "Custom JsonNodeReader must be invoked with the JSON InputFormat; the node it returns should be passed through unchanged."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "key: yamlValue"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            }
          ],
          "expected": "JsonNode returned by custom JsonNodeReader after receiving InputFormat.YAML",
          "description": "Custom JsonNodeReader must be invoked with the YAML InputFormat; the node it returns should be passed through unchanged."
        }
      ]
    },
    {
      "group": "jsonNodeReader is null – null InputFormat handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "{\"valid\":\"json\"}"
            },
            {
              "parameter": "inputFormat",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "When jsonNodeReader is null and the InputFormat argument is null, getObjectMapper(null) will throw a NullPointerException which must be propagated."
        }
      ]
    }
  ]
}