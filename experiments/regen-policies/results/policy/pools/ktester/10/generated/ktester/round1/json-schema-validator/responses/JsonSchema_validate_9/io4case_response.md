{
  "group": "JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)",
  "cases": [
    {
      "input": [
        {
          "parameter": "input",
          "value": "   {\"name\":\"John\"}   "
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
      "description": "Input JSON contains leading and trailing whitespace. The method should trim/ignore whitespace, deserialize successfully and produce no validation messages."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"name\":\"Jöhn\"}"
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
      "description": "Valid JSON containing Unicode characters (non‑ASCII). Verifies that the deserializer correctly handles UTF‑8 input and that validation succeeds."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "[{\"name\":\"John\"}]"
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
      "expected": "set containing a ValidationMessage indicating type mismatch (expected object but got array)",
      "description": "Input is a JSON array while the schema expects an object. The validator should report a type‑mismatch validation message."
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
          "value": "ctx -> ctx.setAttribute(\"custom\", \"value\")"
        }
      ],
      "expected": "set containing a ValidationMessage for type mismatch on property \"name\" and ExecutionContext attribute \"custom\" equals \"value\"",
      "description": "Combines a validation error (type mismatch) with a customizer that mutates the ExecutionContext. Confirms that the customizer is still executed even when validation fails and that its side‑effects are observable."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "\uFEFF{\"name\":\"John\"}"
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
      "description": "Input starts with a UTF‑8 Byte Order Mark (BOM). The deserializer should ignore the BOM and parse the JSON normally."
    },
    {
      "input": [
        {
          "parameter": "input",
          "value": "{\"items\":[{\"name\":\"John0\"},{\"name\":\"John1\"},{\"name\":\"John2\"},{\"name\":\"John3\"},{\"name\":\"John4\"},{\"name\":\"John5\"},{\"name\":\"John6\"},{\"name\":\"John7\"},{\"name\":\"John8\"},{\"name\":\"John9\"}]}"
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
      "description": "A larger, but still valid, JSON document (array of 10 objects). Ensures that the method can handle bigger payloads without error and still returns an empty validation set."
    }
  ]
}