{
  "group": "walk(String, InputFormat, boolean, ExecutionContextCustomizer) - Branch Coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "input",
          "value": null
        },
        {
          "parameter": "inputFormat",
          "value": "JSON"
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
      "expected": "IllegalArgumentException thrown because the input string is null",
      "description": "Verify that the method validates the mandatory `input` argument and throws an exception when it is null (covers the null‑check branch)."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"type\":\"object\"}"
        },
        {
          "parameter": "inputFormat",
          "value": null
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
      "expected": "IllegalArgumentException thrown because the inputFormat is null",
      "description": "Check that a missing `InputFormat` argument is rejected (covers the null‑check branch for the format)."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "invalid json"
        },
        {
          "parameter": "inputFormat",
          "value": "JSON"
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
      "expected": "ValidationResult with isValid() == false and a parsing error message",
      "description": "When deserialization fails (malformed JSON) the method should return a ValidationResult indicating failure (covers the exception‑handling branch inside `deserialize`)."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"type\":\"object\"}"
        },
        {
          "parameter": "inputFormat",
          "value": "JSON"
        },
        {
          "parameter": "validate",
          "value": false
        },
        {
          "parameter": "executionCustomizer",
          "value": null
        }
      ],
      "expected": "ValidationResult with isValid() == true and no ValidationMessage entries",
      "description": "When `validate` flag is false the schema walk should skip validation and return a successful result (covers the `if (validate)` branch)."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"type\":\"object\"}"
        },
        {
          "parameter": "inputFormat",
          "value": "JSON"
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "customizerThatAddsFlag"
        }
      ],
      "expected": "ValidationResult with isValid() == true and the ExecutionContext contains the custom flag set by the customizer",
      "description": "Provide a non‑null `ExecutionContextCustomizer` that mutates the context (e.g., sets a custom attribute). The test verifies that the customizer is invoked (covers the branch where a customizer is present)."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"type\":\"object\"}"
        },
        {
          "parameter": "inputFormat",
          "value": "YAML"
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
      "expected": "ValidationResult with isValid() == true (assuming the YAML input is valid for the schema)",
      "description": "Exercise the path where a different `InputFormat` (YAML) is supplied and validation succeeds (covers the format‑specific deserialization branch)."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"
        },
        {
          "parameter": "inputFormat",
          "value": "JSON"
        },
        {
          "parameter": "validate",
          "value": true
        },
        {
          "parameter": "executionCustomizer",
          "value": "customizerThatThrowsRuntimeException"
        }
      ],
      "expected": "RuntimeException propagated from the ExecutionContextCustomizer",
      "description": "When the customizer itself throws an exception, the method should let the exception bubble up (covers the try‑catch / propagation branch)."
    }
  ]
}