{
  "testCases": [
    {
      "group": "Valid inputs – normal flow",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"name\\\":\\\"John\\\"}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "\"{\\\"valid\\\":true}\"",
          "description": "A well‑formed JSON string is supplied together with a JSON InputFormat and a simple OutputFormat that returns a string representation of the ValidationResult. No ExecutionContextCustomizer is provided, so the default execution context is used. The method should deserialize the input, run validation and return the string produced by the OutputFormat."
        },
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"age\\\":30}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "customizerMock" }
          ],
          "expected": "\"{\\\"valid\\\":true}\"",
          "description": "Same as the previous case but an ExecutionContextCustomizer mock is supplied. The customizer does not alter the context, therefore the validation result is identical to the case without a customizer."
        }
      ]
    },
    {
      "group": "Invalid inputs – null checks",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "null" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "The input string is null. The method should validate arguments and throw IllegalArgumentException (or NullPointerException depending on implementation) before attempting deserialization."
        },
        {
          "input": [
            { "parameter": "input", "value": "\"{}\"" },
            { "parameter": "inputFormat", "value": "null" },
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "InputFormat is null – the method must refuse to continue and raise an IllegalArgumentException."
        },
        {
          "input": [
            { "parameter": "input", "value": "\"{}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "null" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IllegalArgumentException",
          "description": "OutputFormat is null – the method should throw IllegalArgumentException because it cannot produce a result without a format."
        }
      ]
    },
    {
      "group": "Deserialization errors",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{invalidJson:}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "IOException",
          "description": "The supplied string is not valid JSON, causing the private `deserialize` method to throw an IOException. The exception should propagate to the caller."
        },
        {
          "input": [
            { "parameter": "input", "value": "\"%ZZ%\""},
            { "parameter": "inputFormat", "value": "InputFormat.URL_ENCODED"},
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "null" }
          ],
          "expected": "UnsupportedEncodingException",
          "description": "When the InputFormat is URL_ENCODED and the string contains malformed percent‑encoding, `URLDecoder.decode` throws UnsupportedEncodingException. The method must let this exception surface."
        }
      ]
    },
    {
      "group": "ExecutionContextCustomizer effects",
      "cases": [
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"type\\\":\\\"string\\\"}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "customizerAddsFlag" }
          ],
          "expected": "\"{\\\"valid\\\":false,\\\"message\\\":\\\"custom flag triggered\\\"}\"",
          "description": "The customizer mutates the ExecutionContext (e.g., sets a flag `skipValidation=true`). The validator checks this flag and returns a ValidationResult indicating that validation was bypassed. The OutputFormat then returns a string reflecting the bypassed validation."
        },
        {
          "input": [
            { "parameter": "input", "value": "\"{\\\"id\\\":1}\"" },
            { "parameter": "inputFormat", "value": "InputFormat.JSON" },
            { "parameter": "format", "value": "new StringOutputFormat()" },
            { "parameter": "executionCustomizer", "value": "customizerThrowsRuntimeException" }
          ],
          "expected": "RuntimeException",
          "description": "The customizer throws a RuntimeException while customizing the ExecutionContext. The validate method should not catch it, allowing the exception to propagate."
        }
      ]
    }
  ]
}