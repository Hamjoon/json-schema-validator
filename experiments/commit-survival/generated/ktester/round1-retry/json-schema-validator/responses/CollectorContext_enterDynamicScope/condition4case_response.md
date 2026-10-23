{
    "group": "Dynamic Scope Creation",
    "cases": [
        {
            "input": [],
            "expected": "Non‑null Scope instance returned; CollectorContext.getDynamicScope() returns the same instance",
            "description": "Calling the no‑arg enterDynamicScope() on a fresh CollectorContext should create a new dynamic Scope. The returned object must not be null and must be identical to the one obtained via getDynamicScope()."
        },
        {
            "input": [],
            "expected": "Scope instance returned is of type com.networknt.schema.Scope",
            "description": "Verify that the object returned by the no‑arg overload is an instance of the expected Scope class."
        }
    ]
  ,
    "group": "Nested Dynamic Scopes",
    "cases": [
        {
            "input": [],
            "expected": "Two distinct non‑null Scope objects; second call returns a different Scope than the first; getDynamicScope() reflects the latest Scope",
            "description": "Call enterDynamicScope() twice consecutively. The first call creates the initial dynamic scope; the second call should push a new scope, resulting in a different Scope object. The latest scope should be returned by getDynamicScope()."
        },
        {
            "input": [],
            "expected": "Scope stack depth increased by 1 after each call; exitDynamicScope() restores the previous Scope",
            "description": "After two enterDynamicScope() calls, invoke exitDynamicScope() once and verify that getDynamicScope() now returns the first Scope created, confirming proper nesting handling."
        }
    ]
  ,
    "group": "Behaviour with Null Containing Schema",
    "cases": [
        {
            "input": [],
            "expected": "Same behaviour as enterDynamicScope(null); no NullPointerException thrown",
            "description": "The no‑arg method forwards the call to enterDynamicScope(null). Ensure that passing a null containingSchema does not cause an exception and results in a valid Scope."
        }
    ]
  ,
    "group": "State Independence",
    "cases": [
        {
            "input": [],
            "expected": "Scope creation succeeds regardless of whether loadCollectors() has been invoked beforehand",
            "description": "Create a CollectorContext, optionally call loadCollectors() (or skip it), then call enterDynamicScope(). The method should still create a dynamic scope without depending on collector loading."
        }
    ]
}