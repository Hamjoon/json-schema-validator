{
    "group": "Parent Schema Retrieval",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When the validator is instantiated without a parent schema, getParentSchema() should return null."
        },
        {
            "input": [],
            "expected": "mockParentSchema",
            "description": "When a mock JsonSchema is assigned to the protected parentSchema field, getParentSchema() must return the same mock instance (reference equality)."
        },
        {
            "input": [],
            "expected": "realParentSchema",
            "description": "When a real JsonSchema object (created via JsonSchemaFactory) is set as the parent schema, getParentSchema() should return that exact instance."
        }
    ]
}