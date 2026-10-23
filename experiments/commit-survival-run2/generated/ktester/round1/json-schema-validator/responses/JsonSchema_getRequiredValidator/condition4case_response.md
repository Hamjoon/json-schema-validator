{
  "testCases": [
    {
      "group": "Default behavior without required validator",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "A newly created JsonSchema instance has its private field `requiredValidator` initialised to null. Calling `getRequiredValidator()` should therefore return null."
        }
      ]
    },
    {
      "group": "Required validator set via reflection",
      "cases": [
        {
          "input": [
            {
              "parameter": "mockValidator",
              "value": "instance of com.networknt.schema.JsonValidator (e.g., a Mockito mock or a simple stub implementation)"
            }
          ],
          "expected": "same mockValidator instance",
          "description": "Using reflection we assign a non‑null `JsonValidator` instance to the private field `requiredValidator`. After the assignment, `getRequiredValidator()` must return the exact object that was injected."
        }
      ]
    },
    {
      "group": "Required validator set by initialise validators",
      "cases": [
        {
          "input": [],
          "expected": "non‑null JsonValidator instance",
          "description": "When the schema contains a `required` keyword, `initializeValidators()` creates a `RequiredValidator` and stores it in `requiredValidator`. After invoking `initializeValidators()` on a JsonSchema built from such a schema, `getRequiredValidator()` should return a non‑null validator."
        }
      ]
    }
  ]
}