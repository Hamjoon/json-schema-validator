{
  "testCases": [
    {
      "group": "Validation failure – messages present",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.objectNode().put(\"age\", \"twenty\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Integer>() { public Integer apply(ValidationResult r) { return r.getMessages().size(); } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "1",
          "description": "Schema requires an integer for property \"age\". Providing a string causes a validation error. The OutputFormat returns the number of validation messages, which should be 1."
        }
      ]
    },
    {
      "group": "OutputFormat throws RuntimeException – propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.objectNode().put(\"name\", \"John\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<String>() { public String apply(ValidationResult r) { throw new RuntimeException(\"format failure\"); } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "RuntimeException",
          "description": "When the OutputFormat throws a RuntimeException, walk should let the exception bubble up unchanged."
        }
      ]
    },
    {
      "group": "Empty schema – no validators, validation enabled",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.objectNode()"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Integer>() { public Integer apply(ValidationResult r) { return r.getMessages().size(); } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "0",
          "description": "An empty schema (\"{}\") defines no constraints, therefore even with validation enabled the ValidationResult contains zero messages."
        }
      ]
    },
    {
      "group": "Array node validation – type mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.arrayNode().add(1).add(2)"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Integer>() { public Integer apply(ValidationResult r) { return r.getMessages().size(); } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "1",
          "description": "Schema expects an array of strings (items.type = \"string\"). Supplying an array of numbers triggers a validation error; the OutputFormat reports a single message."
        }
      ]
    },
    {
      "group": "ExecutionContext pre‑existing attributes preserved",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "ctxWithExistingAttr"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.objectNode().put(\"valid\", true)"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Boolean>() { public Boolean apply(ValidationResult r) { ExecutionContext ctx = (ExecutionContext) r.getContext(); return Boolean.TRUE.equals(ctx.getAttribute(\"existing\")) && Boolean.TRUE.equals(ctx.getAttribute(\"new\")); } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"new\", true)"
            }
          ],
          "expected": "true",
          "description": "The supplied ExecutionContext already contains attribute \"existing\" = \"value\". The customizer adds a new attribute \"new\" = true. The OutputFormat checks that both attributes are present, confirming that the original attributes are preserved."
        }
      ]
    },
    {
      "group": "Null ExecutionContext with non‑null customizer",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.objectNode().put(\"flag\", true)"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Boolean>() { public Boolean apply(ValidationResult r) { ExecutionContext ctx = (ExecutionContext) r.getContext(); return Boolean.TRUE.equals(ctx.getAttribute(\"custom\")); } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true)"
            }
          ],
          "expected": "true",
          "description": "When the supplied ExecutionContext is null, walk must create a fresh context and then invoke the provided Consumer. The OutputFormat confirms that the attribute set by the customizer exists."
        }
      ]
    },
    {
      "group": "Validation disabled with customizer invoked",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.objectNode().put(\"any\", \"value\")"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Boolean>() { public Boolean apply(ValidationResult r) { ExecutionContext ctx = (ExecutionContext) r.getContext(); return Boolean.TRUE.equals(ctx.getAttribute(\"visited\")); } }"
            },
            {
              "parameter": "validate",
              "value": "false"
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"visited\", true)"
            }
          ],
          "expected": "true",
          "description": "Even when validation is disabled, the Consumer should still be executed. The OutputFormat reads the attribute set by the customizer and returns true."
        }
      ]
    },
    {
      "group": "OutputFormat returns null",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.objectNode()"
            },
            {
              "parameter": "outputFormat",
              "value": "new OutputFormat<Object>() { public Object apply(ValidationResult r) { return null; } }"
            },
            {
              "parameter": "validate",
              "value": "true"
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "If the OutputFormat's apply method returns null, walk should propagate that null result without alteration."
        }
      ]
    }
  ]
}