{
  "newTestCases": [
    {
      "group": "Fail‑fast true with valid instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"name\": \"Alice\", \"age\": 30 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setFailFast(true)"
            }
          ],
          "expected": "Set<ValidationMessage> that is empty (validation succeeds even when fail‑fast is enabled because no error is produced)",
          "description": "The consumer forces fail‑fast mode on the ExecutionContext. The supplied JSON instance satisfies the schema, so the validator should finish without emitting any messages. This case verifies that enabling fail‑fast does not cause a false‑positive failure."
        }
      ]
    },
    {
      "group": "Custom MessageSource localization",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"age\": -1 }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setMessageSource(new MessageSource() { public String getMessage(String key, Object... args) { return \"[FR] \" + key; } })"
            }
          ],
          "expected": "Set<ValidationMessage> containing at least two messages whose text starts with \"[FR] \" (e.g., \"[FR] required.property\" and \"[FR] numeric.minimum\")",
          "description": "A custom MessageSource that prefixes every message with \"[FR] \" is injected via the consumer. The instance violates two constraints (missing required property \"name\" and \"age\" below the minimum). The test confirms that the validator uses the supplied MessageSource for all generated messages."
        }
      ]
    },
    {
      "group": "PathType JSON_POINTER instance location",
      "cases": [
        {
          "input": [
            {
              "parameter": "rootNode",
              "value": "{ \"address\": { \"zip\": \"123\" } }"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setPathType(PathType.JSON_POINTER)"
            }
          ],
          "expected": "Set<ValidationMessage> containing a message whose instanceLocation is \"/address/zip\" (JSON‑Pointer notation) rather than the default JSON‑Path notation (e.g., \"$.address.zip\")",
          "description": "The consumer changes the path representation used in validation messages to JSON‑Pointer. The schema requires \"zip\" to match a numeric pattern, so the provided string is invalid. The test checks that the instanceLocation in the resulting ValidationMessage follows the JSON‑Pointer format, proving that the customizer correctly influences the ExecutionContext."
        }
      ]
    }
  ]
}