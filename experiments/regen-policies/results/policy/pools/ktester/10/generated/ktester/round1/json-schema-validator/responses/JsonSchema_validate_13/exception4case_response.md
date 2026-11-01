{
  "test_cases": [
    {
      "group": "Invalid percent‑encoding – IllegalArgumentException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"%E4%BD%A0%ZZ\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException thrown by URLDecoder when decoding malformed percent‑encoding",
          "description": "The validator URL‑decodes the input using UTF‑8. Supplying an invalid percent‑encoded string should cause URLDecoder.decode to throw IllegalArgumentException, which must propagate out of validate()."
        }
      ]
    },
    {
      "group": "OutputFormat.apply throws runtime exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"a\\\":1}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { throw new IllegalStateException(\"Format failure\"); } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "IllegalStateException propagated from OutputFormat.apply",
          "description": "If the OutputFormat implementation throws an unchecked exception while converting the validation messages, validate() must let the exception bubble up to the caller."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer throws NullPointerException",
      "cases": [
        {
          "input": [
            {
              "parameter": "input",
              "value": "\"{\\\"a\\\":1}\""
            },
            {
              "parameter": "inputFormat",
              "value": "InputFormat.JSON"
            },
            {
              "parameter": "format",
              "value": "new OutputFormat<Object>() { public Object apply(Set<ValidationMessage> msgs) { return msgs; } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new NullPointerException(\"Customizer NPE\"); }"
            }
          ],
          "expected": "NullPointerException propagated from the ExecutionContextCustomizer",
          "description": "When the supplied ExecutionContextCustomizer throws a NullPointerException, validate() should not catch it; the exception must be visible to the caller."
        }
      ]
    }
  ]
}