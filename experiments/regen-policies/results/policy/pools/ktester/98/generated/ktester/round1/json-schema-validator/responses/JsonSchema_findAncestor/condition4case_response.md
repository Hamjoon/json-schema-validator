{
  "testCases": [
    {
      "group": "Root schema (no parent)",
      "cases": [
        {
          "input": [],
          "expected": "the same JsonSchema instance on which findAncestor() is invoked",
          "description": "When the schema has no parent (getParentSchema() returns null), findAncestor() should return the current instance."
        }
      ]
    },
    {
      "group": "Single‑level parent chain",
      "cases": [
        {
          "input": [],
          "expected": "the top‑most parent JsonSchema instance (the root of the chain)",
          "description": "A schema whose getParentSchema() returns another JsonSchema whose own parent is null. findAncestor() must recurse once and return that parent."
        }
      ]
    },
    {
      "group": "Multi‑level parent chain",
      "cases": [
        {
          "input": [],
          "expected": "the ultimate root JsonSchema instance at the head of the chain",
          "description": "A schema with a chain of three or more ancestors (A → B → C → null). findAncestor() should recursively walk the chain and return C, the schema whose parent is null."
        }
      ]
    },
    {
      "group": "Cyclic parent reference (defensive test)",
      "cases": [
        {
          "input": [],
          "expected": "StackOverflowError or a safe termination if implementation guards against cycles",
          "description": "Create two schemas that reference each other as parents (A.getParentSchema() → B, B.getParentSchema() → A). The current implementation has no cycle detection, so invoking findAncestor() will cause infinite recursion leading to StackOverflowError. This test verifies the current behaviour (i.e., that a StackOverflowError is thrown)."
        }
      ]
    }
  ]
}