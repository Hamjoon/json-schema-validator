{
  "testCases": [
    {
      "group": "createSchemaValidatorsConfig - basic non‑null result",
      "cases": [
        {
          "input": [],
          "expected": "non‑null instance of com.networknt.schema.SchemaValidatorsConfig",
          "description": "Verify that the protected method returns a non‑null object of type SchemaValidatorsConfig when invoked via a subclass that exposes the method."
        }
      ]
    },
    {
      "group": "createSchemaValidatorsConfig - distinct instances on multiple calls",
      "cases": [
        {
          "input": [],
          "expected": "two separate SchemaValidatorsConfig objects (different references) with identical default field values",
          "description": "Call the method twice and assert that the returned objects are not the same instance (reference inequality) while their default configuration values are equal, confirming a fresh object is created each time."
        }
      ]
    },
    {
      "group": "createSchemaValidatorsConfig - default field values integrity",
      "cases": [
        {
          "input": [],
          "expected": {
            "failFast": false,
            "typeLoose": false,
            "readOnly": false,
            "additionalProperties": true,
            "format": true,
            "enumCaseSensitive": true,
            "customMessage": null
          },
          "description": "Inspect the newly created SchemaValidatorsConfig and verify that all its configuration flags match the library's documented defaults (example defaults shown). This ensures the factory does not alter the default state."
        }
      ]
    },
    {
      "group": "createSchemaValidatorsConfig - isolation after mutation",
      "cases": [
        {
          "input": [],
          "expected": "first config mutated does not affect second config created afterwards",
          "description": "Obtain a config via the method, modify one of its fields (e.g., setFailFast(true)), then invoke the method again and verify the new config still has the original default (failFast == false). This confirms that the factory returns a fresh, independent configuration each call."
        }
      ]
    }
  ]
}