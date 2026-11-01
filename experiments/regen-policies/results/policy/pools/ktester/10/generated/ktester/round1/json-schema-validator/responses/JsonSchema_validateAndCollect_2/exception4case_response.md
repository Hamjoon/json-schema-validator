{
  "testCases": [
    {
      "group": "Invalid $ref URI causing IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"$ref\":\"ht!tp://[invalid]\"}"
            }
          ],
          "expected": "IllegalArgumentException thrown because the $ref URI is syntactically invalid and URLDecoder fails to parse it",
          "description": "The schema contains a $ref with characters that are not allowed in a URI. When validateAndCollect attempts to resolve the reference it uses URLDecoder, which throws IllegalArgumentException for malformed URIs."
        }
      ]
    },
    {
      "group": "Null ExecutionContext (private overload) triggers NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"name\":\"John\"}"
            }
          ],
          "expected": "NullPointerException thrown when the private validateAndCollect(ExecutionContext, JsonNode, ...) overload is invoked with a null ExecutionContext",
          "description": "Using reflection to call the private overload with a null ExecutionContext should cause the method to dereference the context and throw NPE. This verifies that the method does not silently accept a null context."
        }
      ]
    },
    {
      "group": "External schema reference not found – JsonProcessingException",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"person\":{\"$ref\":\"nonexistent-schema.json#/definitions/name\"}}"
            }
          ],
          "expected": "JsonProcessingException (wrapped in a RuntimeException) because the referenced external schema file cannot be located or parsed",
          "description": "The instance contains a $ref that points to an external JSON schema file that does not exist on the classpath. During resolution the validator attempts to read the file and fails, resulting in a JsonProcessingException."
        }
      ]
    },
    {
      "group": "Custom validator throws unchecked exception – propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"custom\":\"value\"}"
            }
          ],
          "expected": "RuntimeException (e.g., IllegalStateException) propagated out of validateAndCollect because a user‑supplied JsonValidator threw an unchecked exception during validation",
          "description": "A schema is configured with a custom JsonValidator that deliberately throws an IllegalStateException when invoked. The test ensures that validateAndCollect does not swallow the exception but lets it propagate to the caller."
        }
      ]
    },
    {
      "group": "UnsupportedEncodingException during $id decoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{\"$id\":\"http://example.com/schema%ZZ\"}"
            }
          ],
          "expected": "UnsupportedEncodingException thrown because the $id contains an illegal percent‑encoding sequence that URLDecoder cannot decode using the specified charset",
          "description": "The schema defines an $id with malformed percent‑encoding. When validateAndCollect processes the $id it calls URLDecoder.decode with UTF‑8, which throws UnsupportedEncodingException for the illegal sequence."
        }
      ]
    }
  ]
}