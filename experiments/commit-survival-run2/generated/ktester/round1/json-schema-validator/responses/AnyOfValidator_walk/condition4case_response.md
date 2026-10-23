Invalid control character at: line 14 column 45 (char 342)
{
  "testCases": [
    {
      "group": "shouldValidateSchema = true",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class)"
            },
            {
              "parameter": "node",
              "value": "mock(JsonNode.class) // any JSON node"
            },
            {
              "parameter": "rootNode",
              "value": "mock(JsonNode.class) // root of the document"
            },
            {
              "parameter": "at",
              "value": "\"/some/path\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "The method should invoke AnyOfValidator.validate(executionContext, node, rootNode, at) and return exactly the Set<ValidationMessage> produced by that call. No walk calls on the internal schemas list should occur.",
          "description": "When shouldValidateSchema is true the validator must delegate to validate() and return its result."
        }
      ]
    },
    {
      "group": "shouldValidateSchema = false, schemas list empty",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class)"
            },
            {
              "parameter": "node",
              "value": "mock(JsonNode.class)"
            },
            {
              "parameter": "rootNode",
              "value": "mock(JsonNode.class)"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "false"
            }
          ],
          "expected": "An empty LinkedHashSet<> is returned. No schema.walk(...) is invoked because the internal schemas collection is empty.",
          "description": "Verify that with shouldValidateSchema false and no registered schemas the method simply returns an empty set."
        }
      ]
    },
    {
      "group": "shouldValidateSchema = false, multiple schemas",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class)"
            },
            {
              "parameter": "node",
              "value": "mock(JsonNode.class)"
            },
            {
              "parameter": "rootNode",
              "value": "mock(JsonNode.class)"
            },
            {
              "parameter": "at",
              "value": "\"/items/0\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "false"
            }
          ],
          "expected": "An empty LinkedHashSet<> is returned. For each JsonSchema instance present in AnyOfValidator.schemas, its walk(executionContext, node, rootNode, at, false) method is invoked exactly once. The return values of those walks are ignored.",
          "description": "When shouldValidateSchema is false the validator must iterate over all contained schemas, call walk on each with shouldValidateSchema set to false, and finally return an empty set."
        }
      ]
    },
    {
      "group": "shouldValidateSchema = false, schema walk throws exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class)"
            },
            {
              "parameter": "node",
              "value": "mock(JsonNode.class)"
            },
            {
              "parameter": "rootNode",
              "value": "mock(JsonNode.class)"
            },
            {
              "parameter": "at",
              "value": "\"/error\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "false"
            }
          ],
          "expected": "The exception thrown by the schema's walk method propagates out of AnyOfValidator.walk (i.e., the method does not catch it). The returned set is never produced.",
          "description": "Even though the method normally swallows schema walk results, it must not swallow unchecked exceptions; they should be allowed to bubble up."
        }
      ]
    }
  ]
}