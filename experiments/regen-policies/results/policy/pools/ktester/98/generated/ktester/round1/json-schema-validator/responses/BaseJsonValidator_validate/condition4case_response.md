{
  "testCases": [
    {
      "group": "Null handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "null"
            }
          ],
          "expected": "Throws NullPointerException because the method dereferences the node argument before delegating.",
          "description": "Validate should not accept a null JsonNode; the first line of the method (or the overloaded version) will attempt to use the node and therefore a NPE is expected."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            }
          ],
          "expected": "Throws NullPointerException because the executionContext is required for annotation collection and message handling.",
          "description": "Even when a valid JsonNode is supplied, a null ExecutionContext must cause a failure as the method passes the context to internal helpers."
        }
      ]
    },
    {
      "group": "Root handling – atRoot true",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.textNode(\"sample\")"
            }
          ],
          "expected": "Returns the Set<ValidationMessage> produced by the overloaded validate(executionContext, node, rootNode, true) call. In the test subclass the overloaded method is stubbed to return a set containing a single message \"root‑true\".",
          "description": "When atRoot() returns true the delegate call should receive the same node as both instance and root, and the boolean flag true. The test verifies that the returned set matches the stubbed value."
        }
      ]
    },
    {
      "group": "Root handling – atRoot false",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"field\", 1)"
            }
          ],
          "expected": "Returns the Set<ValidationMessage> produced by the overloaded validate(executionContext, node, rootNode, false) call. In the test subclass the overloaded method is stubbed to return a set containing a single message \"root‑false\".",
          "description": "When atRoot() returns false the delegate call should receive the same node as both instance and root, but the boolean flag false. The test confirms that the flag is correctly propagated by checking the stubbed return value."
        }
      ]
    },
    {
      "group": "Exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.arrayNode()"
            }
          ],
          "expected": "RuntimeException with message \"forced‑error\" is thrown and not swallowed.",
          "description": "The overloaded validate method in the test subclass is programmed to throw a RuntimeException for a specific input. The top‑level validate method must propagate this exception unchanged."
        }
      ]
    },
    {
      "group": "Annotation collection branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"annotated\", true)"
            }
          ],
          "expected": "Set<ValidationMessage> returned by overloaded method plus an additional annotation captured via collectAnnotations(true). The test subclass overrides collectAnnotations to return true, indicating that annotation collection succeeded.",
          "description": "validate() internally calls collectAnnotations(executionContext) before delegating. By overriding the method to return true we verify that the path where annotation collection succeeds is exercised."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"annotated\", false)"
            }
          ],
          "expected": "Set<ValidationMessage> returned by overloaded method without any annotation side‑effects because collectAnnotations returns false.",
          "description": "When collectAnnotations returns false the validator skips annotation handling. This case ensures the branch where annotation collection is disabled is covered."
        }
      ]
    }
  ]
}