{
  "group": "JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)",
  "cases": [
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"name\":\"John\"}"
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.JSON"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "empty set of ValidationMessage (no validation errors)",
      "description": "Valid JSON document that satisfies the schema; the consumer does nothing. Verifies the happy‑path where deserialization succeeds and no validation messages are produced."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "name: John"
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.YAML"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> ctx.setAttribute(\"custom\", true)"
        }
      ],
      "expected": "empty set of ValidationMessage and ExecutionContext contains attribute \"custom\" set to true",
      "description": "Valid YAML input; the consumer mutates the ExecutionContext. Confirms that the method correctly selects the YAML deserializer and that the customizer is invoked."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"name\":123}"
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.JSON"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "set containing a ValidationMessage indicating type mismatch for property \"name\"",
      "description": "JSON document violates the schema (name should be a string). Checks that validation messages are returned when the instance does not conform."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{}"
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.JSON"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "set containing a ValidationMessage for missing required property \"name\"",
      "description": "JSON document missing a required property. Ensures that required‑field validation is exercised."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": ""
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.JSON"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "IllegalArgumentException",
      "description": "Empty string input should be rejected early (null/blank check) and result in an IllegalArgumentException."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": null
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.JSON"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "NullPointerException",
      "description": "Null input string triggers a NullPointerException before deserialization."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"name\":\"John\"}"
        },
        {
          "parameter": "inputFormat",
          "value": null
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "NullPointerException",
      "description": "Null InputFormat leads to a NullPointerException when the method attempts to switch on the enum."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"name\":\"John\"}"
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.JSON"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> { throw new RuntimeException(\"customizer failure\"); }"
        }
      ],
      "expected": "RuntimeException with message \"customizer failure\"",
      "description": "The consumer throws an exception; the method should propagate the exception without swallowing it."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{invalid json}"
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.JSON"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "JsonProcessingException wrapped in a RuntimeException (or ValidationException depending on implementation)",
      "description": "Malformed JSON triggers a parsing exception during deserialization; the method should surface the error."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "name: John: extra"
        },
        {
          "parameter": "inputFormat",
          "value": "InputFormat.YAML"
        },
        {
          "parameter": "executionCustomizer",
          "value": "ctx -> {}"
        }
      ],
      "expected": "JsonProcessingException (or equivalent) caused by invalid YAML syntax",
      "description": "Invalid YAML input leads to a deserialization failure; verifies that the YAML branch handles parsing errors correctly."
    }
  ]
}