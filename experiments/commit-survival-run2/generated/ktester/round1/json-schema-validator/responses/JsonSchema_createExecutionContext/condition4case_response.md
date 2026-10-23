{
  "testCases": [
    {
      "group": "createExecutionContext – custom ExecutionContext supplier",
      "cases": [
        {
          "input": [],
          "expected": "The method returns the ExecutionContext instance supplied by the custom ExecutionContextSupplier (the same instance that the supplier returns).",
          "description": "Configure a JsonSchema instance whose ValidationContext returns a SchemaValidatorsConfig whose getExecutionContextSupplier() returns a non‑null Supplier. The Supplier should return a mock ExecutionContext (e.g., using Mockito). Invoke createExecutionContext() and verify that the returned object is exactly the mock supplied by the Supplier, confirming that the `if (config.getExecutionContextSupplier() != null)` branch is taken."
        },
        {
          "input": [],
          "expected": "The method returns an ExecutionContext whose internal CollectorContext has unevaluatedItemsAnalysisDisabled = true and unevaluatedPropertiesAnalysisDisabled = false (as configured in the config).",
          "description": "Configure a JsonSchema instance where the ValidationContext returns a SchemaValidatorsConfig with a null ExecutionContextSupplier, isUnevaluatedItemsAnalysisDisabled() returning true, and isUnevaluatedPropertiesAnalysisDisabled() returning false. Call createExecutionContext() and assert that the returned ExecutionContext contains a CollectorContext constructed with those two flag values. This covers the `else` branch where a default ExecutionContext is created."
        },
        {
          "input": [],
          "expected": "The method returns an ExecutionContext whose CollectorContext flags are both false when the config disables neither analysis.",
          "description": "Same as the previous case but both config flags are false. Verify that the CollectorContext inside the returned ExecutionContext reflects false for both flags, ensuring that the constructor arguments are correctly passed."
        },
        {
          "input": [],
          "expected": "The method returns an ExecutionContext whose CollectorContext flags are both true when the config disables both analyses.",
          "description": "Configure the config to return true for both isUnevaluatedItemsAnalysisDisabled() and isUnevaluatedPropertiesAnalysisDisabled(). After invoking createExecutionContext(), verify that the CollectorContext inside the ExecutionContext has both flags set to true. This tests the path where the flags are varied."
        }
      ]
    }
  ]
}