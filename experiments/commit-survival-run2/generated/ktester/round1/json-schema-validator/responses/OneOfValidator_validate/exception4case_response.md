{
  "test_cases": [
    {
      "group": "OneOfValidator.validate - null executionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "null"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "at",
              "value": "\"/path\""
            }
          ],
          "expected": "NullPointerException",
          "description": "When executionContext is null the method attempts to call executionContext.getCollectorContext() and throws NullPointerException."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - executionContext with null collectorContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(withCollectorContext=null, withValidatorState(walkEnabled=false, validationEnabled=true))"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "at",
              "value": "\"/path\""
            }
          ],
          "expected": "NullPointerException",
          "description": "CollectorContext is null; the first call to collectorContext.enterDynamicScope() results in NullPointerException."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - null schema element in list",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(validNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "at",
              "value": "\"/path\""
            }
          ],
          "expected": "NullPointerException",
          "description": "The validator's internal schema list contains a null entry; invoking schema.validate() on a null reference throws NullPointerException."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - schema.validate throws RuntimeException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "at",
              "value": "\"/path\""
            }
          ],
          "expected": "RuntimeException",
          "description": "A schema in the OneOfValidator throws a RuntimeException during validate(). The exception propagates out of the method (no catch), resulting in a RuntimeException."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - schema.walk throws RuntimeException (walk enabled)",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=true, validationEnabled=true))"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "at",
              "value": "\"/path\""
            }
          ],
          "expected": "RuntimeException",
          "description": "When walk is enabled, the validator calls schema.walk(). The mocked schema throws a RuntimeException, which propagates out of validate()."
        }
      ]
    },
    {
      "group": "OneOfValidator.validate - null 'at' parameter",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext(withCollectorContext, withValidatorState(walkEnabled=false, validationEnabled=true))"
            },
            {
              "parameter": "node",
              "value": "mockJsonNode(anyNode)"
            },
            {
              "parameter": "rootNode",
              "value": "mockJsonNode(root)"
            },
            {
              "parameter": "at",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "The 'at' argument is used in buildValidationMessage(at, ...). Passing null leads to a NullPointerException inside that helper."
        }
      ]
    }
  ]
}