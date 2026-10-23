{
  "testCases": [
    {
      "group": "fetchSubSchemaNode - obtainSubSchemaNode throws NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext (any value)"
            }
          ],
          "expected": "NullPointerException propagated",
          "description": "When suppressSubSchemaRetrieval is false and the static obtainSubSchemaNode method is forced (via mocking) to throw a NullPointerException (e.g., because an internal null reference is accessed), fetchSubSchemaNode must not catch it and let the exception bubble up."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - obtainSubSchemaNode throws IllegalStateException",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext (any value)"
            }
          ],
          "expected": "IllegalStateException propagated",
          "description": "If obtainSubSchemaNode detects an illegal validator state (e.g., missing required schema attributes) and throws IllegalStateException, fetchSubSchemaNode should propagate the exception unchanged."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - obtainSubSchemaNode throws URISyntaxException (wrapped as IllegalArgumentException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext with malformed base URI \"http://[invalid]\""
            }
          ],
          "expected": "IllegalArgumentException (caused by URISyntaxException) propagated",
          "description": "When the ValidationContext contains a malformed URI, obtainSubSchemaNode may attempt to resolve a $ref and throw a URISyntaxException wrapped in IllegalArgumentException. fetchSubSchemaNode must not swallow it."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - obtainSubSchemaNode throws ClassCastException",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext (any value)"
            }
          ],
          "expected": "ClassCastException propagated",
          "description": "If schemaNode is of an unexpected JSON node type (e.g., an ArrayNode) and obtainSubSchemaNode casts it to ObjectNode, a ClassCastException may be thrown. fetchSubSchemaNode must let this exception propagate."
        }
      ]
    },
    {
      "group": "fetchSubSchemaNode - obtainSubSchemaNode throws IOException",
      "cases": [
        {
          "input": [
            {
              "parameter": "validationContext",
              "value": "mocked ValidationContext (any value)"
            }
          ],
          "expected": "IOException propagated",
          "description": "When obtainSubSchemaNode reads an external schema reference and encounters an I/O problem, it can throw IOException. fetchSubSchemaNode should not catch it, allowing the caller to handle the failure."
        }
      ]
    }
  ]
}