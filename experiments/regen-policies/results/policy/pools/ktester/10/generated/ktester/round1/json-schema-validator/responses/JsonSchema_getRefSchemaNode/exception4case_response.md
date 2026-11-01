{
  "testCases": [
    {
      "group": "Null reference argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling getRefSchemaNode with a null reference should cause a NullPointerException when the method attempts to evaluate ref.startsWith(...)."
        }
      ]
    },
    {
      "group": "Schema resource root is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties/name"
            }
          ],
          "expected": "NullPointerException",
          "description": "If findSchemaResourceRoot() returns null, the subsequent call to schema.getSchemaNode() dereferences a null object, resulting in a NullPointerException."
        }
      ]
    },
    {
      "group": "Schema node is null",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/properties/name"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the schema returned by findSchemaResourceRoot() has a null schema node (getSchemaNode() returns null), the method attempts to call node.at(...), which throws a NullPointerException."
        }
      ]
    }
  ]
}