{
  "testCases": [
    {
      "group": "Immutable evaluated properties collection when disabled",
      "cases": [
        {
          "input": [],
          "expected": "Collection size remains 0 after attempting to add \"propX\"; add operation returns false; iterator is empty",
          "description": "Create a CollectorContext with unevaluatedProperties disabled (e.g., new CollectorContext(true) or using a builder that sets disableUnevaluatedProperties = true). Call enterDynamicScope(), retrieve the collection via getEvaluatedProperties(), attempt collection.add(\"propX\") and verify that the method returns false, collection.size() is still 0, and collection.iterator().hasNext() is false. This confirms that the returned collection is immutable when the feature is disabled."
        }
      ]
    },
    {
      "group": "Evaluated properties per nested dynamic scope",
      "cases": [
        {
          "input": [],
          "expected": "Two different Collection instances are returned for two nested scopes; after exiting the inner scope the original instance is restored",
          "description": "1. Enter a first dynamic scope (scope1) via context.enterDynamicScope(). 2. Retrieve collection A = context.getEvaluatedProperties(). 3. Enter a nested dynamic scope (scope2) via context.enterDynamicScope(). 4. Retrieve collection B = context.getEvaluatedProperties(). 5. Assert that A != B (different object identity). 6. Exit the inner scope via context.exitDynamicScope(). 7. Retrieve collection C = context.getEvaluatedProperties() and assert C == A. This validates that each dynamic scope owns its own evaluated‑properties collection and that the context correctly switches back to the outer scope."
        }
      ]
    },
    {
      "group": "Merging evaluated properties from child scope",
      "cases": [
        {
          "input": [],
          "expected": "Parent scope's collection contains both \"parentProp\" and \"childProp\" after merge",
          "description": "1. Enter a parent dynamic scope (parent) and add \"parentProp\" to its evaluated properties (parent.getEvaluatedProperties().add(\"parentProp\")). 2. Enter a child dynamic scope (child) and add \"childProp\" (child.getEvaluatedProperties().add(\"childProp\")). 3. Merge child into parent by invoking parent.mergeWith(child) (available through CollectorContext by calling parentScope.mergeWith(childScope) or via context.enterDynamicScope() returning the Scope objects). 4. Verify that parent.getEvaluatedProperties() contains both \"parentProp\" and \"childProp\" and that its size is 2. This tests the mergeWith implementation and ensures that getEvaluatedProperties() reflects merged data."
        }
      ]
    },
    {
      "group": "Evaluated properties collection available without explicit dynamic scope",
      "cases": [
        {
          "input": [],
          "expected": "Non‑null, mutable collection is returned; size is 0 initially",
          "description": "Instantiate CollectorContext with default settings (unevaluatedProperties enabled). Without calling enterDynamicScope(), invoke getEvaluatedProperties(). Verify that the returned collection is not null, its size is 0, and that adding an element (e.g., \"rootProp\") succeeds (size becomes 1). This confirms that the context provides a default (top) scope even when the user has not explicitly entered a dynamic scope."
        }
      ]
    }
  ]
}