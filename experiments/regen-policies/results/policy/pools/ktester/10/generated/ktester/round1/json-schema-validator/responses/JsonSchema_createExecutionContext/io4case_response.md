{
  "testCases": [
    {
      "group": "createExecutionContext_additional_localeAndFormatFlag",
      "cases": [
        {
          "input": [],
          "expected": "ExecutionContext is returned whose ExecutionConfig.locale equals Locale.GERMAN and ExecutionConfig.formatAssertionsEnabled equals false (as provided by the SchemaValidatorsConfig). No customizer is invoked, so the ExecutionContext remains otherwise untouched.",
          "description": "Validate that when the ValidationContext's config specifies a non‑default locale (German) and disables format assertions, the created ExecutionContext correctly reflects those values and no customization occurs."
        }
      ]
    },
    {
      "group": "createExecutionContext_additional_unevaluatedFlags",
      "cases": [
        {
          "input": [],
          "expected": "ExecutionContext is returned whose CollectorContext.unevaluatedItemsAnalysisDisabled and CollectorContext.unevaluatedPropertiesAnalysisDisabled are both true, matching the flags set in SchemaValidatorsConfig. ExecutionConfig values are default (locale null, formatAssertionsEnabled false) because they were not overridden.",
          "description": "Ensure that the CollectorContext constructed inside createExecutionContext copies the unevaluated‑items and unevaluated‑properties analysis disable flags from the configuration."
        }
      ]
    },
    {
      "group": "createExecutionContext_additional_customizerModifiesLocale",
      "cases": [
        {
          "input": [],
          "expected": "ExecutionContext is returned where the ExecutionConfig.locale has been changed to Locale.FRENCH by the ExecutionContextCustomizer. The customizer receives the newly created ExecutionContext and the original ValidationContext.",
          "description": "Test that a non‑null ExecutionContextCustomizer is invoked and can mutate the ExecutionContext (e.g., overriding the locale). The test asserts that the locale after customization matches the value set by the customizer."
        }
      ]
    },
    {
      "group": "createExecutionContext_additional_customizerThrowsException",
      "cases": [
        {
          "input": [],
          "expected": "RuntimeException is thrown and propagates out of createExecutionContext, because the provided ExecutionContextCustomizer throws an exception during customization.",
          "description": "Verify that createExecutionContext does not swallow exceptions from the customizer; any RuntimeException thrown by the customizer should be propagated to the caller."
        }
      ]
    }
  ]
}