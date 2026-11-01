{
  "testCases": [
    {
      "group": "jsonNodeReader is null – default ObjectMapper branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "InputStream containing JSON text: {\"name\":\"John\"}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "JsonNode returned by ObjectMapper.readTree (mocked)",
          "description": "When jsonNodeReader is null and the input format is JSON, the method should delegate to the JSON ObjectMapper and return the parsed JsonNode."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "InputStream containing YAML text: name: John"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            }
          ],
          "expected": "JsonNode returned by YamlMapper.readTree (mocked)",
          "description": "When jsonNodeReader is null and the input format is YAML, the method should delegate to the YAML ObjectMapper and return the parsed JsonNode."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "InputStream that throws IOException when read"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "IOException is propagated",
          "description": "If the underlying ObjectMapper.readTree throws IOException, the method must propagate the exception."
        }
      ]
    },
    {
      "group": "jsonNodeReader is not null – JsonNodeReader branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "InputStream containing JSON text: {\"age\":30}"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "JsonNode returned by JsonNodeReader.readTree (mocked)",
          "description": "When jsonNodeReader is present, the method should delegate to jsonNodeReader.readTree regardless of the format and return its JsonNode."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "InputStream containing YAML text: age: 30"
            },
            {
              "parameter": "inputFormat",
              "value": "YAML"
            }
          ],
          "expected": "JsonNode returned by JsonNodeReader.readTree (mocked)",
          "description": "When jsonNodeReader is present and the format is YAML, the method should delegate to jsonNodeReader.readTree and return its JsonNode."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "InputStream that causes JsonNodeReader to throw IOException"
            },
            {
              "parameter": "inputFormat",
              "value": "JSON"
            }
          ],
          "expected": "IOException is propagated",
          "description": "If JsonNodeReader.readTree throws IOException, the method must propagate the exception."
        }
      ]
    }
  ]
}