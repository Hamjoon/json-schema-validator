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
            },
            {
              "parameter": "rootNode",
              "value": "null"
            },
            {
              "parameter": "at",
              "value": "\"\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "false"
            }
          ],
          "expected": "ValidationResult with isValid()==false and a ValidationMessage indicating that the node is null",
          "description": "When all node parameters are null the method should return an invalid ValidationResult rather than throwing NPE."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class)"
            },
            {
              "parameter": "node",
              "value": "null"
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
              "value": "true"
            }
          ],
          "expected": "ValidationResult with isValid()==false and a ValidationMessage stating \"instance is missing\"",
          "description": "When the instance node is null but validation is requested, the method should report a missing instance error."
        }
      ]
    },
    {
      "group": "Validation flag handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/data\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "false"
            }
          ],
          "expected": "ValidationResult.isValid()==true (no validation performed)",
          "description": "When shouldValidateSchema is false the walk should succeed without invoking any validator."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class)"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/data\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==true (schema validated and no errors)",
          "description": "When validation is enabled and the node satisfies the schema, the method returns a valid result."
        }
      ]
    },
    {
      "group": "Required validator branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithRequired()))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()" 
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/person\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a message about missing required property",
          "description": "Schema contains a required property that is absent in the instance; the required validator should be triggered."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithRequired()))"
            },
            {
              "parameter": "node",
              "value": "objectNode().put(\"name\", \"John\")"
            },
            {
              "parameter": "rootNode",
              "value": "objectNode().put(\"name\", \"John\")"
            },
            {
              "parameter": "at",
              "value": "\"/person\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==true (required validator passes)",
          "description": "All required properties are present, so the required validator should not add any errors."
        }
      ]
    },
    {
      "group": "Type validator branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithType(\"string\")))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.numberNode(10)"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.numberNode(10)"
            },
            {
              "parameter": "at",
              "value": "\"/age\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a type‑mismatch message",
          "description": "The schema expects a string but the instance is a number; the type validator should report an error."
        },
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithType(\"integer\")))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.numberNode(5)"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.numberNode(5)"
            },
            {
              "parameter": "at",
              "value": "\"/count\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==true (type validator passes)",
          "description": "Instance type matches the schema type, so no type‑related errors are produced."
        }
      ]
    },
    {
      "group": "Dynamic anchor handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenReturn(schemaWithDynamicAnchor()))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==true and the walk registers the dynamic anchor in the ExecutionContext",
          "description": "When the schema is marked as a dynamic anchor, walkAtNode should store the anchor location without causing validation failures."
        }
      ]
    },
    {
      "group": "Exception handling / unsupported encoding",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getSchema()).thenThrow(new UnsupportedEncodingException(\"UTF‑8 not supported\")))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==false and contains a ValidationMessage wrapping UnsupportedEncodingException",
          "description": "If an internal call throws UnsupportedEncodingException, walkAtNode should catch it and return a failed ValidationResult rather than propagating the exception."
        }
      ]
    },
    {
      "group": "Keyword walk listener runner branch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mock(ExecutionContext.class, with -> when(with.getKeywordWalkListenerRunner()).thenReturn(new DefaultKeywordWalkListenerRunner()))"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "rootNode",
              "value": "JsonNodeFactory.instance.objectNode()"
            },
            {
              "parameter": "at",
              "value": "\"/\""
            },
            {
              "parameter": "shouldValidateSchema",
              "value": "true"
            }
          ],
          "expected": "ValidationResult.isValid()==true and the listener runner's beforeWalk/afterWalk hooks are invoked",
          "description": "When a KeywordWalkListenerRunner is present, walkAtNode should delegate to it and ensure the callbacks are executed."
        }
      ]
    }
  ]
}