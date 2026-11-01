{
  "group": "BaseJsonValidator#getParentSchema",
  "cases": [
    {
      "input": [],
      "expected": "null",
      "description": "When the BaseJsonValidator instance has no parent schema assigned (parentSchema == null), getParentSchema() should return null."
    },
    {
      "input": [],
      "expected": "instance of JsonSchema (mockParentSchema)",
      "description": "After assigning a mock JsonSchema to the protected parentSchema field of a concrete subclass, getParentSchema() must return the same mock instance."
    },
    {
      "input": [],
      "expected": "instance of JsonSchema (secondParentSchema)",
      "description": "If the parentSchema field is changed after construction (e.g., via reflection or a setter in a subclass), getParentSchema() should reflect the new value and return the updated JsonSchema instance."
    }
  ]
}