{
  "testCases": [
    {
      "group": "walk_whenShouldValidateSchemaTrue",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/path\"" },
            { "parameter": "shouldValidateSchema", "value": "true" }
          ],
          "expected": "Set containing a single ValidationMessage with message 'anyOf validation failed'",
          "description": "When shouldValidateSchema is true the method must delegate to validate() and return whatever Set<ValidationMessage> validate() produces. The test uses a subclass of AnyOfValidator that overrides validate() to return a known Set, verifying that walk() returns exactly that Set."
        }
      ]
    },
    {
      "group": "walk_whenShouldValidateSchemaFalse_noSchemas",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/empty\"" },
            { "parameter": "shouldValidateSchema", "value": "false" }
          ],
          "expected": "Empty LinkedHashSet",
          "description": "When shouldValidateSchema is false and the internal schemas list is empty, walk() should iterate over an empty collection, call no schema.walk(), and return an empty Set."
        }
      ]
    },
    {
      "group": "walk_whenShouldValidateSchemaFalse_withSchemas",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/multiple\"" },
            { "parameter": "shouldValidateSchema", "value": "false" }
          ],
          "expected": "Empty LinkedHashSet and each schema's walk() invoked exactly once with shouldValidateSchema=false",
          "description": "When shouldValidateSchema is false and the validator holds several JsonSchema objects, walk() must invoke walk() on each contained schema with the same node, rootNode, at and shouldValidateSchema=false, then return an empty Set. The test verifies the invocation count using mocks."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "at", "value": "\"/nullSchemaList\"" },
            { "parameter": "shouldValidateSchema", "value": "false" }
          ],
          "expected": "Empty LinkedHashSet and no schema.walk() called (schemas list is null or uninitialized)",
          "description": "Edge case where the internal schemas list is null (simulated via reflection) – walk() should behave like an empty list and simply return an empty Set without throwing NPE."
        }
      ]
    }
  ]
}