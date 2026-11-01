{
  "group": "createSchemaValidatorsConfig",
  "cases": [
    {
      "input": [],
      "expected": "no exception thrown",
      "description": "Invoking createSchemaValidatorsConfig() should complete without throwing any exception."
    },
    {
      "input": [],
      "expected": "second call returns a config whose default values are unchanged after the first config is mutated",
      "description": "Modify a property (e.g., setFailFast(true)) on the first returned SchemaValidatorsConfig and then call createSchemaValidatorsConfig() again. The new instance must still have the library's default value for that property (failFast = false), proving that each call returns an independent object."
    },
    {
      "input": [],
      "expected": "empty collections for mutable fields (e.g., formatValidators map size == 0)",
      "description": "The freshly created SchemaValidatorsConfig should contain empty mutable collections such as the custom format validators map. Verify that the collection size is zero, confirming no shared state."
    },
    {
      "input": [],
      "expected": "instance of exactly class com.networknt.schema.SchemaValidatorsConfig",
      "description": "Check that the object returned by createSchemaValidatorsConfig() is an instance of the concrete class SchemaValidatorsConfig and not a subclass or proxy."
    },
    {
      "input": [],
      "expected": "all instances obtained from concurrent calls are distinct (different references)",
      "description": "Execute createSchemaValidatorsConfig() concurrently from multiple threads (e.g., using ParallelStream or ExecutorService). Collect the results and assert that each instance has a unique reference, ensuring thread‑safe creation of fresh configurations."
    }
  ]
}