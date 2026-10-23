{
  "testCases": [
    {
      "group": "Empty tuple schema",
      "cases": [
        {
          "input": [],
          "expected": "An empty List instance (size 0) returned by getTupleSchema()",
          "description": "Validate that when PrefixItemsValidator is constructed with an empty tupleSchema, getTupleSchema() returns a non‑null List with zero elements."
        }
      ]
    },
    {
      "group": "Single element tuple schema",
      "cases": [
        {
          "input": [],
          "expected": "A List containing exactly the provided JsonSchema mock instance (size 1)",
          "description": "Validate that when PrefixItemsValidator is constructed with a List that holds one JsonSchema mock, getTupleSchema() returns that same List preserving the element."
        }
      ]
    },
    {
      "group": "Multiple elements tuple schema",
      "cases": [
        {
          "input": [],
          "expected": "A List containing the three supplied JsonSchema mock instances in the same order (size 3)",
          "description": "Validate that when PrefixItemsValidator is constructed with a List of several JsonSchema objects, getTupleSchema() returns the exact List with all elements in the original order."
        }
      ]
    },
    {
      "group": "Mutable reference test",
      "cases": [
        {
          "input": [],
          "expected": "After adding a new JsonSchema to the List returned by getTupleSchema(), the internal tupleSchema of the validator reflects the added element (size increased).",
          "description": "Confirm that getTupleSchema() returns a live reference to the internal list, not a copy, by mutating the returned list and checking the validator’s internal state."
        }
      ]
    },
    {
      "group": "Null tuple schema handling",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Using reflection to set the private field `tupleSchema` to null, verify that getTupleSchema() returns null (no NullPointerException is thrown by the method itself)."
        }
      ]
    }
  ]
}