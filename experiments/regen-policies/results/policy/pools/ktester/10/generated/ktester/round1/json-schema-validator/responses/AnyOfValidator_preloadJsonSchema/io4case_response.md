{
  "testCases": [
    {
      "group": "preloadJsonSchema - single schema list",
      "cases": [
        {
          "input": [],
          "expected": "The method `preloadJsonSchemas` is invoked exactly once with the internal `schemas` list containing the single mock `JsonSchema` instance; after the call the list still contains that instance (no change).",
          "description": "Create an `AnyOfValidator` instance, inject one mocked `JsonSchema` into its private `schemas` field, then call `preloadJsonSchema()`. Verify via Mockito that `preloadJsonSchemas` receives the list with the single schema and that the list content remains unchanged."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - list containing null element",
      "cases": [
        {
          "input": [],
          "expected": "The method `preloadJsonSchemas` is invoked exactly once with the internal `schemas` list that includes a `null` entry; no exception is thrown by `preloadJsonSchema()` itself.",
          "description": "Populate the `schemas` list with a valid mock `JsonSchema` followed by a `null` reference. Invoke `preloadJsonSchema()` and verify that the call is delegated unchanged (using a spy) and that the list still contains the `null` element after the call."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - duplicate schemas in list",
      "cases": [
        {
          "input": [],
          "expected": "`preloadJsonSchemas` is called once with the list containing the duplicate `JsonSchema` objects; the internal list remains unchanged (duplicates stay).",
          "description": "Add the same mock `JsonSchema` instance twice to the private `schemas` list. Call `preloadJsonSchema()` and verify that the delegate method receives the list with both entries and that the validator does not deduplicate or alter the list."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - multiple invocations (idempotence)",
      "cases": [
        {
          "input": [],
          "expected": "`preloadJsonSchemas` is invoked exactly twice – once for each call to `preloadJsonSchema()`. The internal `schemas` list is unchanged after each call.",
          "description": "Prepare an `AnyOfValidator` with two mocked schemas. Call `preloadJsonSchema()` two consecutive times. Using a Mockito spy, verify that `preloadJsonSchemas` is called twice with the same list instance and that the list content is identical after each invocation."
        }
      ]
    },
    {
      "group": "preloadJsonSchema - list mutation by preloadJsonSchemas",
      "cases": [
        {
          "input": [],
          "expected": "After the call, the internal `schemas` list contains the original schemas plus the additional schema added inside the overridden `preloadJsonSchemas` implementation.",
          "description": "Create a subclass of `AnyOfValidator` that overrides `preloadJsonSchemas(List<JsonSchema>)` to add a new mock `JsonSchema` to the provided list. Populate the original `schemas` list with two mocks, invoke `preloadJsonSchema()`, and assert that the list size has increased to three and includes the newly added schema."
        }
      ]
    }
  ]
}