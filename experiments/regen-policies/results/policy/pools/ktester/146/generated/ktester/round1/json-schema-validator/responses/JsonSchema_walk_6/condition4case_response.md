Invalid control character at: line 10 column 47 (char 236)
{
  "testCases": [
    {
      "group": "Validate flag true – normal execution",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext() // fresh context with default settings"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"age\", 30) // valid instance for the schema"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> ctx.setAttribute(\"custom\", true) // simple consumer that mutates the context"
            }
          ],
          "expected": "ValidationResult.success() with no ValidationMessage; the ExecutionContext should contain attribute \"custom\" set to true",
          "description": "When validate is true the method must run the full validation chain and return a successful ValidationResult. The consumer must be invoked exactly once, mutating the context."
        }
      ]
    },
    {
      "group": "Validate flag false – walk without validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"age\", \"not-a-number\") // violates the schema but validation is disabled"
            },
            {
              "parameter": "validate",
              "value": false
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {} // no‑op consumer"
            }
          ],
          "expected": "ValidationResult.success() (no validation errors) because the validate flag is false; the consumer must still be called.",
          "description": "The method should bypass all validator execution when the validate flag is false, returning a successful result regardless of node content."
        }
      ]
    },
    {
      "group": "Null executionCustomizer – default handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"age\", 25)"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "null"
            }
          ],
          "expected": "ValidationResult.success(); no NullPointerException is thrown. The method must treat a null consumer as a no‑op.",
          "description": "The implementation should guard against a null Consumer and behave as if a no‑op consumer was supplied."
        }
      ]
    },
    {
      "group": "Null ExecutionContext – defensive check",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"age\", 30)"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException thrown with message containing \"executionContext\"",
          "description": "The method must validate that the ExecutionContext argument is not null and throw an NPE if it is."
        }
      ]
    },
    {
      "group": "Null JsonNode – defensive check",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "null"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> {}"
            }
          ],
          "expected": "NullPointerException thrown with message containing \"node\"",
          "description": "A null JsonNode is illegal; the method should fail fast with an NPE."
        }
      ]
    },
    {
      "group": "Consumer throws exception – propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"age\", 30)"
            },
            {
              "parameter": "validate",
              "value": true
            },
            {
              "parameter": "executionCustomizer",
              "value": "ctx -> { throw new IllegalStateException(\"customizer failure\"); }"
            }
          ],
          "expected": "IllegalStateException propagated out of walk(...)",
          "description": "If the supplied consumer throws, the method must not swallow the exception; it should propagate it to the caller."
        }
      ]
    }
  ]
}