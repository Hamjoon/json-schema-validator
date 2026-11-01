{
  "testCases": [
    {
      "group": "Null schema resource root handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/any/pointer"
            }
          ],
          "expected": "NullPointerException",
          "description": "When findSchemaResourceRoot() returns null, the subsequent call to schema.getId() triggers a NullPointerException before any other processing."
        }
      ]
    },
    {
      "group": "Null schema node handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "ref",
              "value": "/some/path"
            }
          ],
          "expected": "NullPointerException",
          "description": "findSchemaResourceRoot() returns a valid JsonSchema, but its getSchemaNode() returns null. Because the ref starts with '/', the method attempts node.at(jsonPointer) on a null node, causing a NullPointerException."
        }
      ]
    }
  ]
}