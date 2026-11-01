{
  "tests": [
    {
      "group": "Custom SchemaLoader via Builder",
      "cases": [
        {
          "input": [],
          "expected": "the custom SchemaLoader instance supplied to the builder is returned by getSchemaLoader()",
          "description": "Create a JsonSchemaFactory using JsonSchemaFactory.builder().schemaLoader(customLoader).build() where customLoader is a stub implementation of SchemaLoader. Verify that getSchemaLoader() returns the exact same instance (reference equality)."
        },
        {
          "input": [],
          "expected": "the same custom SchemaLoader instance is returned on multiple calls",
          "description": "After building the factory with a custom SchemaLoader, invoke getSchemaLoader() several times and assert that each call returns the identical object, confirming the getter does not recreate the loader."
        }
      ]
    },
    {
      "group": "SchemaLoader inheritance via blueprint builder",
      "cases": [
        {
          "input": [],
          "expected": "the SchemaLoader from the blueprint factory is reused in the new factory",
          "description": "Create a first factory (blueprint) with a custom SchemaLoader. Then create a second factory via JsonSchemaFactory.builder(blueprint).build(). Verify that the second factory's getSchemaLoader() returns the same SchemaLoader instance that the blueprint factory holds."
        },
        {
          "input": [],
          "expected": "modifying the custom SchemaLoader after blueprint creation does not affect the new factory's loader",
          "description": "After the blueprint factory is built, replace its custom loader reference with a different stub (if mutable) and ensure the factory created from the blueprint still returns the original loader, proving the builder copies the reference at construction time."
        }
      ]
    },
    {
      "group": "Custom SchemaLoader via getInstance customizer",
      "cases": [
        {
          "input": [],
          "expected": "the SchemaLoader supplied in the customizer is returned by getSchemaLoader()",
          "description": "Call JsonSchemaFactory.getInstance(VersionFlag.V201909, builder -> builder.schemaLoader(customLoader)). Verify that the resulting factory's getSchemaLoader() returns the customLoader instance."
        },
        {
          "input": [],
          "expected": "the custom SchemaLoader remains the same across repeated getInstance calls with identical customizer",
          "description": "Invoke getInstance with the same version flag and identical customizer twice and assert that both factories return the same custom SchemaLoader instance (reference equality), confirming the customizer is applied consistently."
        }
      ]
    },
    {
      "group": "Default SchemaLoader for different SpecVersion flags",
      "cases": [
        {
          "input": [],
          "expected": "a non‑null DefaultSchemaLoader instance is returned for each supported VersionFlag",
          "description": "Iterate over all values of SpecVersion.VersionFlag (e.g., V4, V6, V7, V201909). For each flag, obtain a factory via JsonSchemaFactory.getInstance(flag) and verify that getSchemaLoader() returns a non‑null instance of DefaultSchemaLoader."
        },
        {
          "input": [],
          "expected": "different VersionFlag values produce distinct SchemaLoader instances",
          "description": "Create two factories with different VersionFlag values (e.g., V4 and V7) and assert that the SchemaLoader objects returned are not the same instance, ensuring the loader is scoped per version."
        }
      ]
    }
  ]
}