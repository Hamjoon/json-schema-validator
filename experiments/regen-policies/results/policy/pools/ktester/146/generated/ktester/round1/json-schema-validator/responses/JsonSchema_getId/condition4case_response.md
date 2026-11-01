{
  "testCases": [
    {
      "group": "JsonSchema#getId - basic values",
      "cases": [
        {
          "input": [],
          "expected": "schema1",
          "description": "Verify that getId() returns the exact id string that was supplied to the JsonSchema constructor (normal non‑empty value)."
        },
        {
          "input": [],
          "expected": "",
          "description": "Verify that getId() correctly returns an empty string when the JsonSchema was constructed with an empty id."
        },
        {
          "input": [],
          "expected": null,
          "description": "Verify that getId() returns null when the JsonSchema was constructed with a null id (allowed because the field is final but may be null)."
        },
        {
          "input": [],
          "expected": "http://example.com/schema#fragment",
          "description": "Verify that getId() returns a complex URI‑like id containing special characters and a fragment identifier."
        },
        {
          "input": [],
          "expected": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "description": "Verify that getId() correctly returns a long id string (100+ characters) without truncation or alteration."
        }
      ]
    },
    {
      "group": "JsonSchema#getId - immutability",
      "cases": [
        {
          "input": [],
          "expected": "immutable-id",
          "description": "Call getId() twice on the same JsonSchema instance and ensure both calls return the identical string, confirming the id field is immutable after construction."
        }
      ]
    }
  ]
}