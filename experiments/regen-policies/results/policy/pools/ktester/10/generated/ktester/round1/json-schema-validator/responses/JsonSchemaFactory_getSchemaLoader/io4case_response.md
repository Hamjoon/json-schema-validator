{
  "tests": [
    {
      "group": "JsonSchemaFactory.getSchemaLoader - default instance",
      "cases": [
        {
          "input": [],
          "expected": "a non‑null object of type com.networknt.schema.SchemaLoader",
          "description": "Create a JsonSchemaFactory using the default builder (no custom SchemaLoader supplied). Call getSchemaLoader() and verify that it returns a non‑null instance whose runtime type implements SchemaLoader. This confirms that the factory provides a sensible default loader when none is injected."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchemaLoader - unchanged after schema retrieval",
      "cases": [
        {
          "input": [],
          "expected": "the same SchemaLoader instance that was originally returned",
          "description": "Obtain a JsonSchemaFactory via the default builder, store the reference returned by getSchemaLoader(), then invoke a typical factory method such as getSchema(\"{\\\"type\\\":\\\"string\\\"}\") which parses a simple schema. After the call, invoke getSchemaLoader() again and assert that the returned reference is identical (==) to the one captured before the schema retrieval. This ensures that other factory operations do not replace or recreate the loader."
        }
      ]
    },
    {
      "group": "JsonSchemaFactory.getSchemaLoader - thread‑safety",
      "cases": [
        {
          "input": [],
          "expected": "all concurrently‑executed calls return the identical SchemaLoader reference",
          "description": "Create a JsonSchemaFactory (default builder). Start N (e.g., 10) threads that each invoke getSchemaLoader() and store the result in a thread‑local variable. After all threads complete, verify that every stored reference is == to the reference obtained in the main thread before starting the workers. This validates that getSchemaLoader() is safe to call from multiple threads and does not produce distinct loader instances."
        }
      ]
    }
  ]
}