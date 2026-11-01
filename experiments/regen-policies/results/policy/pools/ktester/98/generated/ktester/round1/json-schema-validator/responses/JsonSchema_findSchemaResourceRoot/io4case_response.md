{
  "testCases": [
    {
      "group": "Nearest resource root among multiple true ancestors",
      "cases": [
        {
          "input": [],
          "expected": "returns the immediate parent JsonSchema instance whose isSchemaResourceRoot() returns true, even though higher ancestors also return true",
          "description": "Create a chain of three JsonSchema objects where the child reports false, the direct parent reports true, and the grand‑parent also reports true. The method should stop at the first true ancestor (the direct parent) and return it, confirming that it does not continue searching for a higher‑level resource root."
        }
      ]
    },
    {
      "group": "Deep hierarchy with resource root at depth 5",
      "cases": [
        {
          "input": [],
          "expected": "returns the JsonSchema instance located at depth 5 (the first ancestor with isSchemaResourceRoot() == true)",
          "description": "Build a chain of six JsonSchema objects (child → level1 → level2 → level3 → level4 → root). Only the topmost object (depth 5 from the child) reports true for isSchemaResourceRoot(). The method must iterate through five parent links and return that root instance, demonstrating correct operation over a deeper hierarchy."
        }
      ]
    },
    {
      "group": "Self‑referencing schema does not cause infinite loop (detects cycle)",
      "cases": [
        {
          "input": [],
          "expected": "throws StackOverflowError due to infinite recursion caused by a parent reference that points to the same instance",
          "description": "Configure a JsonSchema instance whose getParentSchema() returns itself and whose isSchemaResourceRoot() returns false. Invoking findSchemaResourceRoot() will cause the while‑loop to never reach a true condition, eventually resulting in a StackOverflowError. This test verifies that the method does not contain protective cycle detection."
        }
      ]
    }
  ]
}