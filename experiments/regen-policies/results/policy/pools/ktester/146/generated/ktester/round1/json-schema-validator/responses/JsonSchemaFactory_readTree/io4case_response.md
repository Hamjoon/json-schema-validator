{
  "testCases": [
    {
      "group": "jsonNodeReader is null – edge cases with null or empty inputs",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "IOException (or JsonProcessingException) thrown by ObjectMapper.readTree due to null content",
          "description": "When jsonNodeReader is null and the supplied content is null, ObjectMapper.readTree should throw an exception which propagates out of readTree."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "IOException (or JsonProcessingException) thrown by ObjectMapper.readTree because the input string is empty",
          "description": "Validate that an empty JSON string results in a parsing exception when delegating to the default ObjectMapper."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "   "
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "IOException (or JsonProcessingException) thrown by ObjectMapper.readTree due to whitespace‑only content",
          "description": "Even though the format is YAML, whitespace‑only input cannot be parsed and should cause an exception that bubbles up."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "{\"valid\":true}"
            },
            {
              "parameter": "inputFormat",
              "value": null
            }
          ],
          "expected": "NullPointerException thrown before delegating to ObjectMapper",
          "description": "If the InputFormat argument is null, the method should fail early (NPE) because getObjectMapper(null) cannot resolve a mapper."
        }
      ]
    },
    {
      "group": "jsonNodeReader is set – edge cases with null/empty inputs and null returns",
      "cases": [
        {
          "input": [
            {
              "parameter": "content",
              "value": null
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "IOException thrown by JsonNodeReader.readTree due to null content",
          "description": "When a custom JsonNodeReader is injected, passing a null content string should cause the reader to throw an IOException, which must propagate unchanged."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": ""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "JsonNode returned by JsonNodeReader (could be null or EmptyNode depending on mock configuration)",
          "description": "Test that the factory forwards an empty string to the injected JsonNodeReader; the mock is set up to return a specific JsonNode (e.g., NullNode) and the factory returns exactly that node."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "{\"key\":\"value\"}"
            },
            {
              "parameter": "inputFormat",
              "value": null
            }
          ],
          "expected": "NullPointerException thrown before calling JsonNodeReader.readTree",
          "description": "When the InputFormat argument is null, the method should not invoke the JsonNodeReader and instead throw NPE (or behave consistently with the null‑format contract)."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "{\"nested\":{\"a\":1,\"b\":2}}"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            }
          ],
          "expected": "Mocked JsonNode representing a complex nested structure",
          "description": "Ensure that the factory returns exactly the JsonNode supplied by the JsonNodeReader even when the node contains nested objects; verifies no transformation occurs."
        },
        {
          "input": [
            {
              "parameter": "content",
              "value": "any string"
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.YAML"
            }
          ],
          "expected": "null (JsonNodeReader returns null)",
          "description": "If the injected JsonNodeReader deliberately returns null, readTree should propagate that null result rather than throwing or wrapping it."
        }
      ]
    }
  ]
}