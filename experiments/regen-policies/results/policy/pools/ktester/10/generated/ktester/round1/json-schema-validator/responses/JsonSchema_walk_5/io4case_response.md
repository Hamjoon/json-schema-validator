{
  "test_cases": [
    {
      "group": "NullKeywordWalkListenerRunner",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_noRunner"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "NullPointerException is thrown before any validator walk or post‑walk listener execution because keywordWalkListenerRunner is null.",
          "description": "The JsonSchema instance has its keywordWalkListenerRunner field left null. The walk method attempts to call runPreWalkListeners, which should raise a NullPointerException. This verifies that the method does not silently ignore a missing runner."
        }
      ]
    },
    {
      "group": "MultipleValidatorsMixedResults",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_twoValidators"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "Returned error set containing only the messages from the second validator; first validator returned an empty set, so its scope is merged into the parent scope.",
          "description": "JsonSchema contains two validators. The first validator returns an empty Set, the second returns a non‑empty Set of ValidationMessage. The walk method must aggregate errors from the second validator only and still merge the first validator's dynamic scope into the parent."
        }
      ]
    },
    {
      "group": "PostWalkListenersInvokedOnSkip",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_skipPreWalkPostCheck"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": false
            }
          ],
          "expected": "Returned empty error set; validator walk body not executed; post‑walk listeners are still invoked exactly once per validator.",
          "description": "Pre‑walk listeners return false (skip). The test verifies that, despite the skip, the finally block still triggers runPostWalkListeners for each validator."
        }
      ]
    },
    {
      "group": "EmptyValidatorsList",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_noValidators"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "Returned empty error set; no pre‑walk or post‑walk listeners are called because there are no validators.",
          "description": "The schema's getValidators() method returns an empty list. The walk method should simply return an empty LinkedHashSet without invoking any listeners."
        }
      ]
    },
    {
      "group": "PreWalkListenerThrowsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_preWalkException"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "RuntimeException propagates after post‑walk listeners are executed; no error set is returned.",
          "description": "A pre‑walk listener throws a RuntimeException. The finally block must still invoke runPostWalkListeners before the exception bubbles up, mirroring the behavior tested for validator‑thrown exceptions."
        }
      ]
    },
    {
      "group": "EvaluatedItemsNotAddedWhenNoErrors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_itemsValidatorEmptyResult"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": true
            }
          ],
          "expected": "Returned empty error set; collectorContext.evaluatedItems remains unchanged because the ItemsValidator returned an empty result.",
          "description": "Validator is an instance of ItemsValidator (or PrefixItemsValidator/ItemsValidator202012/ContainsValidator) but its walk method returns an empty Set. The method must NOT copy evaluated items from the child scope into the collector context."
        }
      ]
    },
    {
      "group": "EvaluatedPropertiesNotAddedWhenNoErrors",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "mockExecutionContext_propertiesValidatorEmptyResult"
            },
            {
              "parameter": "node",
              "value": "mockNode"
            },
            {
              "parameter": "rootNode",
              "value": "mockRootNode"
            },
            {
              "parameter": "instanceLocation",
              "value": "mockJsonNodePath"
            },
            {
              "parameter": "shouldValidateSchema",
              "value": false
            }
          ],
          "expected": "Returned empty error set; collectorContext.evaluatedProperties remains unchanged because the PropertiesValidator returned an empty result.",
          "description": "Validator is an instance of PropertiesValidator (or AdditionalPropertiesValidator/PatternPropertiesValidator) and returns an empty Set. The evaluatedProperties set must not be populated."
        }
      ]
    }
  ]
}