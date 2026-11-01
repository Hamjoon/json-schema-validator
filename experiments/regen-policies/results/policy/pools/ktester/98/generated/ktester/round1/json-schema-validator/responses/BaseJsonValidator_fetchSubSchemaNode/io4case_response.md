{
  "group": "BaseJsonValidator.fetchSubSchemaNode.additional",
  "cases": [
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "null"
        }
      ],
      "expected": "NullPointerException",
      "description": "When the supplied ValidationContext is null the method should immediately throw a NullPointerException because it is passed to the private static obtainSubSchemaNode method."
    },
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "Mocked ValidationContext (no special behavior required)"
        }
      ],
      "expected": "null",
      "description": "suppressSubSchemaRetrieval = false, but the schemaNode is a non‑object JsonNode (e.g., a textual node). The private obtainSubSchemaNode implementation treats such nodes as non‑schema and returns null, so fetchSubSchemaNode must also return null."
    },
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "Mocked ValidationContext (no special behavior required)"
        }
      ],
      "expected": "non‑null JsonSchema instance (JsonSchemaImpl)",
      "description": "suppressSubSchemaRetrieval = false and the schemaNode contains a valid $ref that resolves to an actual schema. By mocking the static obtainSubSchemaNode method (using PowerMockito/Mockito) to return a specific JsonSchemaImpl instance, the test verifies that fetchSubSchemaNode returns exactly that instance, confirming proper delegation."
    },
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "Mocked ValidationContext (no special behavior required)"
        }
      ],
      "expected": "null",
      "description": "Even when suppressSubSchemaRetrieval = true, the internal schemaNode is non‑null (e.g., an ObjectNode with a $ref). The method must still short‑circuit and return null, demonstrating that the flag overrides any possible schema resolution."
    },
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "Mocked ValidationContext (no special behavior required)"
        }
      ],
      "expected": "non‑null JsonSchema instance",
      "description": "suppressSubSchemaRetrieval = false and the schemaNode is a valid ObjectNode (no $ref). The obtainSubSchemaNode method creates a JsonSchemaImpl from the node. The test asserts that the returned schema is non‑null and that its getSchemaNode() equals the original schemaNode, confirming that the node is correctly wrapped."
    },
    {
      "input": [
        {
          "parameter": "validationContext",
          "value": "Mocked ValidationContext (no special behavior required)"
        }
      ],
      "expected": "validationContext unchanged (no interactions)",
      "description": "When suppressSubSchemaRetrieval = false, fetchSubSchemaNode should not mutate the supplied ValidationContext. Using a Mockito spy, the test verifies that no methods on the ValidationContext are invoked during the call."
    }
  ]
}