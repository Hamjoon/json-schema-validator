{
  "testCases": [
    {
      "group": "createExecutionContext – default supplier with mixed flags (items disabled, properties enabled)",
      "cases": [
        {
          "input": [],
          "expected": "The method returns an ExecutionContext whose internal CollectorContext has unevaluatedItemsAnalysisDisabled = false and unevaluatedPropertiesAnalysisDisabled = true, matching the configuration supplied via SchemaValidatorsConfig.",
          "description": "Configure a JsonSchema instance where ValidationContext returns a SchemaValidatorsConfig with a null ExecutionContextSupplier, isUnevaluatedItemsAnalysisDisabled() returning false and isUnevaluatedPropertiesAnalysisDisabled() returning true. Call createExecutionContext() and verify that the CollectorContext inside the returned ExecutionContext reflects those flag values. This covers the previously untested combination of flag values."
        }
      ]
    },
    {
      "group": "createExecutionContext – supplier returns null",
      "cases": [
        {
          "input": [],
          "expected": "The method returns null because the custom ExecutionContextSupplier supplied in the config returns null.",
          "description": "Provide a SchemaValidatorsConfig whose getExecutionContextSupplier() returns a Supplier that itself returns null. Invoke createExecutionContext() and assert that the returned value is null, confirming that the method does not replace a null supplier result with a default ExecutionContext."
        }
      ]
    },
    {
      "group": "createExecutionContext – supplier throws exception",
      "cases": [
        {
          "input": [],
          "expected": "The method propagates the RuntimeException thrown by the custom ExecutionContextSupplier.",
          "description": "Configure a SchemaValidatorsConfig with a non‑null ExecutionContextSupplier that throws a RuntimeException when get() is called. Call createExecutionContext() and verify that the same exception bubbles up, ensuring that the method does not swallow supplier exceptions."
        }
      ]
    },
    {
      "group": "createExecutionContext – multiple invocations produce distinct instances",
      "cases": [
        {
          "input": [],
          "expected": "Two successive calls to createExecutionContext() (with a null supplier) return two different ExecutionContext objects, each containing its own CollectorContext.",
          "description": "Set up a JsonSchema with a config that has a null ExecutionContextSupplier and known flag values (e.g., both false). Invoke createExecutionContext() twice and assert that the returned ExecutionContext instances are not the same object (using !=) and that each contains a non‑null CollectorContext. This validates that a fresh ExecutionContext is created on each call when no supplier is provided."
        }
      ]
    },
    {
      "group": "createExecutionContext – validationContext returns null config",
      "cases": [
        {
          "input": [],
          "expected": "A NullPointerException is thrown because validationContext.getConfig() returns null and the method attempts to dereference it.",
          "description": "Mock the ValidationContext of a JsonSchema to return null for getConfig(). Invoke createExecutionContext() and verify that a NullPointerException is thrown, confirming the method's behaviour when the configuration is absent."
        }
      ]
    }
  ]
}