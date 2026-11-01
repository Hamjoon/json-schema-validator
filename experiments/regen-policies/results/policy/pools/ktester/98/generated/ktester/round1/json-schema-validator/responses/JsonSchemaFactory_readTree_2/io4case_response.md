{
  "testCases": [
    {
      "group": "inputFormat null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "new ByteArrayInputStream('{\"x\":1}'.getBytes(StandardCharsets.UTF_8))"
            },
            {
              "parameter": "inputFormat",
              "value": "null"
            }
          ],
          "expected": "NullPointerException (or IllegalArgumentException) thrown by getObjectMapper when inputFormat is null",
          "description": "When jsonNodeReader is null and the caller passes a null InputFormat, the factory should not silently accept it – the underlying getObjectMapper call is expected to raise an NPE (or a specific IllegalArgumentException). The exception must propagate to the caller."
        }
      ]
    },
    {
      "group": "jsonNodeReader throws unchecked exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "new ByteArrayInputStream('{\"a\":true}'.getBytes(StandardCharsets.UTF_8))"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "IllegalStateException thrown by the mocked JsonNodeReader propagates unchanged",
          "description": "If a custom JsonNodeReader implementation throws an unchecked exception (e.g., IllegalStateException) while reading the stream, JsonSchemaFactory.readTree must not catch it; the exception should bubble up to the caller."
        }
      ]
    },
    {
      "group": "jsonNodeReader returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "new ByteArrayInputStream('{\"nullNode\":null}'.getBytes(StandardCharsets.UTF_8))"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "null (the method returns the null value produced by JsonNodeReader)",
          "description": "When a non‑null JsonNodeReader returns null (simulating a reader that decides the content cannot be parsed), JsonSchemaFactory.readTree should simply return null without throwing an exception."
        }
      ]
    },
    {
      "group": "format/content mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "new ByteArrayInputStream('key: value'.getBytes(StandardCharsets.UTF_8))"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "IOException (or JsonProcessingException) thrown because JSON mapper cannot parse YAML content",
          "description": "Passing YAML text while requesting JSON parsing should cause the underlying ObjectMapper to fail and throw an IOException."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "new ByteArrayInputStream('{\"k\":\"v\"}'.getBytes(StandardCharsets.UTF_8))"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "JsonNode representing an object with field \"k\" equal to \"v\" (YAML parser accepts JSON as a subset)",
          "description": "YAML parser in Jackson can parse valid JSON. This test verifies that when the format is YAML but the content is JSON, the method still succeeds and returns the correct JsonNode."
        }
      ]
    },
    {
      "group": "input stream read error",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": "new InputStream() {\n    @Override public int read() throws IOException { throw new IOException(\"forced read error\"); }\n}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "IOException thrown by ObjectMapper.readTree due to underlying stream read failure",
          "description": "When the supplied InputStream throws an IOException during read, the factory method must let that exception propagate (it is not caught internally)."
        }
      ]
    }
  ]
}