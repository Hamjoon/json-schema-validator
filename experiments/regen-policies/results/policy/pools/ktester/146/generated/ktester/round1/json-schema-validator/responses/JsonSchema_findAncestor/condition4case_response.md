{
  "testCases": [
    {
      "group": "findAncestor - no parent (root schema)",
      "cases": [
        {
          "input": [],
          "expected": "Returns the same instance (the root schema itself)",
          "description": "A JsonSchema instance whose parent schema is null should return itself when findAncestor() is invoked. This exercises the false branch of the `if (this.getParentSchema() != null)` condition."
        }
      ]
    },
    {
      "group": "findAncestor - single level parent",
      "cases": [
        {
          "input": [],
          "expected": "Returns the direct parent schema instance",
          "description": "Create a child JsonSchema whose parent is a root schema (parent.getParentSchema() == null). Calling findAncestor() on the child should traverse one level and return the parent, covering the true branch of the `if` and the subsequent return of the ancestor."
        }
      ]
    },
    {
      "group": "findAncestor - multi‑level parent chain",
      "cases": [
        {
          "input": [],
          "expected": "Returns the top‑most ancestor (root schema) instance",
          "description": "Construct a chain of three schemas: leaf → middle → root, where only the root has a null parent. Invoking findAncestor() on the leaf triggers recursive calls through the true branch multiple times and ultimately returns the root schema."
        }
      ]
    },
    {
      "group": "findAncestor - self‑referencing parent (cycle detection)",
      "cases": [
        {
          "input": [],
          "expected": "Throws StackOverflowError",
          "description": "Force a cycle by setting a schema's parent to itself. The method will repeatedly call findAncestor() on the same instance, eventually causing a StackOverflowError. This validates that the method does not guard against cyclic parent references."
        }
      ]
    }
  ]
}