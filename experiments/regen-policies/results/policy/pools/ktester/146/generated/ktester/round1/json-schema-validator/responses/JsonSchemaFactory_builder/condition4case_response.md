{
  "testCases": [
    {
      "group": "builder() basic functionality",
      "cases": [
        {
          "input": [],
          "expected": "non-null Builder instance",
          "description": "Calling JsonSchemaFactory.builder() should return a non‑null Builder object."
        },
        {
          "input": [],
          "expected": "different Builder instances on successive calls",
          "description": "Two consecutive calls to JsonSchemaFactory.builder() must return distinct Builder objects (different references)."
        }
      ]
    },
    {
      "group": "builder() build produces JsonSchemaFactory",
      "cases": [
        {
          "input": [],
          "expected": "non-null JsonSchemaFactory instance",
          "description": "Invoking build() on the Builder returned by JsonSchemaFactory.builder() should yield a non‑null JsonSchemaFactory."
        },
        {
          "input": [],
          "expected": "independent configurations between builders",
          "description": "Configure one Builder (e.g., set a custom SchemaValidatorsConfig via its API), build a factory, then create a second Builder without that configuration and build another factory. The two factories must be distinct and the second must not inherit the custom configuration of the first."
        }
      ]
    }
  ]
}