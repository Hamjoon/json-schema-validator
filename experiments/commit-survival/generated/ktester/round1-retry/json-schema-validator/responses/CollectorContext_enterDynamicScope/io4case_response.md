{
  "testCases": [
    {
      "group": "Default Scope Creation",
      "cases": [
        {
          "input": [],
          "expected": "A non‑null Scope instance is returned and CollectorContext.getDynamicScope() reflects this new scope",
          "description": "Invoke enterDynamicScope() with no arguments and verify that the returned Scope object is not null and becomes the current dynamic scope of the CollectorContext."
        }
      ]
    },
    {
      "group": "Explicit Null Parameter",
      "cases": [
        {
          "input": [
            {
              "parameter": "containingSchema",
              "value": null
            }
          ],
          "expected": "A non‑null Scope instance is returned, identical in behaviour to the no‑arg overload",
          "description": "Call the overloaded enterDynamicScope(null) and assert that it creates a Scope just like the default method (no‑arg) and that the current dynamic scope is set accordingly."
        }
      ]
    },
    {
      "group": "Scope With Containing Schema",
      "cases": [
        {
          "input": [
            {
              "parameter": "containingSchema",
              "value": "mockJsonSchema"
            }
          ],
          "expected": "A non‑null Scope instance is returned that records the supplied JsonSchema",
          "description": "Pass a mock JsonSchema object to enterDynamicScope(JsonSchema) and verify that the returned Scope is not null and that the scope internally references the provided schema (e.g., via a getContainingSchema() accessor if available)."
        }
      ]
    },
    {
      "group": "Nested Dynamic Scopes",
      "cases": [
        {
          "input": [],
          "expected": "Two distinct Scope objects are created; the second scope’s parent (or previous) is the first scope",
          "description": "Invoke enterDynamicScope() twice in succession. Verify that each call returns a distinct Scope instance and that the second scope is linked to the first (e.g., via a getParent() method or by checking that getDynamicScope() after the second call returns the second scope while the first is still reachable as its predecessor)."
        }
      ]
    },
    {
      "group": "Exit Returns Previous Scope",
      "cases": [
        {
          "input": [],
          "expected": "Calling exitDynamicScope() after two enters returns the first entered Scope",
          "description": "Enter two dynamic scopes, then call exitDynamicScope(). The method should return the Scope that was active before the most recent enter (i.e., the first Scope). After exit, CollectorContext.getDynamicScope() should point to that first Scope."
        }
      ]
    },
    {
      "group": "Exit Without Prior Enter Throws",
      "cases": [
        {
          "input": [],
          "expected": "IllegalStateException (or a similar runtime exception) is thrown",
          "description": "Create a fresh CollectorContext and directly invoke exitDynamicScope() without any prior enterDynamicScope() call. The method is expected to signal improper state by throwing an exception."
        }
      ]
    },
    {
      "group": "Multiple Exits Return To Null",
      "cases": [
        {
          "input": [],
          "expected": "After exiting all entered scopes, CollectorContext.getDynamicScope() returns null",
          "description": "Enter a dynamic scope, exit it, then call exitDynamicScope() again. The second exit should either throw an exception or leave the dynamic scope as null; the test asserts that the current dynamic scope is null after all scopes have been closed."
        }
      ]
    }
  ]
}