{
  "testCases": [
    {
      "group": "PreWalkListenerSkip",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_skipPreWalk" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Returned empty error set; walk body not executed; only post‑walk listeners invoked",
          "description": "Pre‑walk listeners return false (skip). The method should bypass the validator walk loop body and directly invoke post‑walk listeners, resulting in an empty error set."
        }
      ]
    },
    {
      "group": "PreWalkListenerProceed",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_proceedPreWalk" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "Returned empty error set; validator walk executed; post‑walk listeners invoked",
          "description": "Pre‑walk listeners return true, so the validator walk is performed. The validator returns null, which makes the collector merge scopes without adding errors."
        }
      ]
    },
    {
      "group": "ValidatorReturnsNullOrEmpty",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_nullResult" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Returned empty error set; collectorContext merges parent scope with child scope (no errors added)",
          "description": "Validator walk returns null. The method should treat it like an empty result, merge the dynamic scopes and return an empty error set."
        },
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_emptyResult" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Returned empty error set; collectorContext merges parent scope with child scope (no errors added)",
          "description": "Validator walk returns an empty Set. The method should merge scopes and not add any errors."
        }
      ]
    },
    {
      "group": "ValidatorReturnsErrors",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_withErrors" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Returned error set containing the validator's messages; collectorContext does NOT merge parent scope with child scope",
          "description": "Validator walk returns a non‑empty Set of ValidationMessage. The method should add those messages to the final error set and skip merging the parent scope with the child scope."
        }
      ]
    },
    {
      "group": "ValidatorSpecificTypes_Items",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_itemsValidator" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Returned error set (if any); collectorContext.evaluatedItems contains items from child scope after walk",
          "description": "Validator is an instance of ItemsValidator (or PrefixItemsValidator / ItemsValidator202012 / ContainsValidator). After a non‑empty result, the method must copy evaluated items from the child scope into CollectorContext.evaluatedItems."
        }
      ]
    },
    {
      "group": "ValidatorSpecificTypes_Properties",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_propertiesValidator" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": false }
          ],
          "expected": "Returned error set (if any); collectorContext.evaluatedProperties contains properties from child scope after walk",
          "description": "Validator is an instance of PropertiesValidator (or AdditionalPropertiesValidator / PatternPropertiesValidator). After a non‑empty result, the method must copy evaluated properties from the child scope into CollectorContext.evaluatedProperties."
        }
      ]
    },
    {
      "group": "ExceptionDuringValidatorWalk",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "mockExecutionContext_exception" },
            { "parameter": "node", "value": "mockNode" },
            { "parameter": "rootNode", "value": "mockRootNode" },
            { "parameter": "instanceLocation", "value": "mockJsonNodePath" },
            { "parameter": "shouldValidateSchema", "value": true }
          ],
          "expected": "Method propagates the exception after post‑walk listeners are executed; error set is not returned",
          "description": "Validator walk throws a RuntimeException. The finally block must still invoke post‑walk listeners before the exception bubbles up."
        }
      ]
    }
  ]
}