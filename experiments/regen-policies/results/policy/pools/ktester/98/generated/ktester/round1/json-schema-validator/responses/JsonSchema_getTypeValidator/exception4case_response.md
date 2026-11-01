{
    "test_cases": [
        {
            "group": "Exception propagation when getValidators throws NullPointerException",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Create a subclass of JsonSchema that overrides getValidators() to throw a NullPointerException. With validators initially null, invoking getTypeValidator() should trigger the overridden getValidators() and the NullPointerException must propagate out of getTypeValidator()."
                }
            ]
        },
        {
            "group": "Exception propagation when getValidators throws IllegalArgumentException",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "Create a subclass of JsonSchema that overrides getValidators() to throw an IllegalArgumentException (e.g., because the schema contains an unsupported keyword). Calling getTypeValidator() on a fresh instance (validators == null) must cause the IllegalArgumentException to be thrown by getTypeValidator()."
                }
            ]
        },
        {
            "group": "Exception propagation when getValidators throws custom unchecked exception",
            "cases": [
                {
                    "input": [],
                    "expected": "SchemaException",
                    "description": "Define a custom unchecked exception class SchemaException extending RuntimeException. Subclass JsonSchema and override getValidators() to throw new SchemaException(\"custom error\"). When getTypeValidator() is invoked, the SchemaException should be propagated unchanged."
                }
            ]
        },
        {
            "group": "Error propagation when getValidators throws OutOfMemoryError",
            "cases": [
                {
                    "input": [],
                    "expected": "OutOfMemoryError",
                    "description": "Subclass JsonSchema and override getValidators() to throw an OutOfMemoryError. Invoking getTypeValidator() must let the Error bubble up, demonstrating that getTypeValidator() does not catch Errors."
                }
            ]
        }
    ]
}