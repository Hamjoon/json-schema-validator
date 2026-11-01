{
    "tests": [
        {
            "group": "getValidationContext() - after static factory `from`",
            "cases": [
                {
                    "input": [],
                    "expected": "the same ValidationContext instance that was supplied to JsonSchema.from(...) (reference equality)",
                    "description": "Create a ValidationContext, a dummy SchemaLocation, JsonNodePath, and a simple JsonNode representing a schema. Call the package‑private static method JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false) via reflection. The returned JsonSchema should expose the exact ValidationContext instance through getValidationContext()."
                }
            ]
        },
        {
            "group": "getValidationContext() - child schema inherits parent context",
            "cases": [
                {
                    "input": [],
                    "expected": "the same ValidationContext instance as the parent schema (reference equality)",
                    "description": "Build a parent JsonSchema using the `from` factory with a known ValidationContext. Then create a child schema by calling parent.getSubSchema(JsonNodePath.of(\"#/properties/child\")) (or invoke the private constructor that accepts a parent). Verify that child.getValidationContext() returns the identical ValidationContext object that was given to the parent."
                }
            ]
        },
        {
            "group": "getValidationContext() - after applying configuration with `withConfig`",
            "cases": [
                {
                    "input": [],
                    "expected": "the original ValidationContext instance (reference equality) – configuration does not replace it",
                    "description": "Instantiate a JsonSchema (via `from`) with a known ValidationContext. Call schema.withConfig(new SchemaValidatorsConfig()) which returns a new JsonSchema instance. Ensure that the new instance’s getValidationContext() still returns the original ValidationContext object, confirming that configuration changes do not create a new ValidationContext."
                }
            ]
        }
    ]
}