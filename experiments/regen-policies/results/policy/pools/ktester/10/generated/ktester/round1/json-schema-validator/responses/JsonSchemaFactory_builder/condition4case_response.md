{
  "group": "JsonSchemaFactory.builder() method",
  "cases": [
    {
      "input": [],
      "expected": "non‑null Builder instance",
      "description": "A call to JsonSchemaFactory.builder() must return a newly created Builder object; the result should not be null."
    },
    {
      "input": [],
      "expected": "two distinct Builder instances",
      "description": "Two successive calls to JsonSchemaFactory.builder() must produce two different Builder objects (different references), confirming that a fresh instance is created each time."
    }
  ]
}