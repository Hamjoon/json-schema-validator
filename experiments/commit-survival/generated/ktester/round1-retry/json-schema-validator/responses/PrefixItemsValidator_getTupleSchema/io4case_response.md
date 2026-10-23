{
  "testCases": [
    {
      "group": "Original list mutation effect",
      "cases": [
        {
          "input": [],
          "expected": "The List returned by getTupleSchema() reflects the element added to the original List after the PrefixItemsValidator was constructed (size increased).",
          "description": "Create a mutable List, construct PrefixItemsValidator with it, then add a new JsonSchema to the original List. Verify that getTupleSchema() returns a List that now includes the newly added element, demonstrating that the validator holds a live reference to the supplied list."
        }
      ]
    },
    {
      "group": "Reference equality across calls",
      "cases": [
        {
          "input": [],
          "expected": "Two successive calls to getTupleSchema() return the exact same List instance (reference equality).",
          "description": "Invoke getTupleSchema() twice on the same PrefixItemsValidator instance and assert that the returned objects are `==` (the same reference), confirming that the method does not create a new List on each call."
        }
      ]
    },
    {
      "group": "Immutable list behavior",
      "cases": [
        {
          "input": [],
          "expected": "Calling add() on the List returned by getTupleSchema() throws UnsupportedOperationException.",
          "description": "Construct PrefixItemsValidator with `Collections.unmodifiableList(new ArrayList<>())`. Retrieve the tuple schema via getTupleSchema() and attempt to modify it. The operation should fail with an UnsupportedOperationException, showing that the validator does not wrap or copy the list."
        }
      ]
    },
    {
      "group": "List containing null element",
      "cases": [
        {
          "input": [],
          "expected": "A List of size 1 containing a null entry is returned by getTupleSchema().",
          "description": "Pass a List that contains a single `null` element to the PrefixItemsValidator constructor. Verify that getTupleSchema() returns a non‑null List whose sole element is `null`, confirming that the validator does not filter out null entries."
        }
      ]
    },
    {
      "group": "List with duplicate schemas",
      "cases": [
        {
          "input": [],
          "expected": "A List containing the duplicate JsonSchema instances in the order supplied (size reflects duplicates).",
          "description": "Create a List with the same JsonSchema mock instance added multiple times, construct the validator, and assert that getTupleSchema() returns a List preserving the duplicates and order, proving that no deduplication occurs."
        }
      ]
    }
  ]
}