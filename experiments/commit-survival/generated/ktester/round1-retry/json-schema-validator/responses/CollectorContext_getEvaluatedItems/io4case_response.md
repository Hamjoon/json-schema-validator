{
    "newTestCases": [
        {
            "group": "Entering a new dynamic scope creates a distinct collection",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "outerCollectionNotNull": true,
                        "innerCollectionNotNull": true,
                        "differentInstance": true,
                        "outerInitialSize": 0,
                        "innerInitialSize": 0
                    },
                    "description": "Create a CollectorContext with default (enabled) unevaluated items. Capture the collection returned by getEvaluatedItems() (outer). Call enterDynamicScope() to push a new Scope and capture the collection again (inner). Verify both collections are non‑null, have size 0, and are different objects (inner is a fresh collection)."
                }
            ]
        },
        {
            "group": "Exiting a dynamic scope restores the previous collection",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "outerCollectionNotNull": true,
                        "innerCollectionNotNull": true,
                        "outerContainsAdded": false,
                        "innerContainsAdded": true,
                        "afterExitContainsAdded": false,
                        "sameInstanceAfterExit": true
                    },
                    "description": "Start with a CollectorContext (default). Obtain outer collection. Enter a new dynamic scope, obtain inner collection and add a marker element \"x\" via inner.add(\"x\"). Exit the dynamic scope. Verify that the collection returned by getEvaluatedItems() after exit is the original outer collection (same instance) and does not contain the element added in the inner scope."
                }
            ]
        },
        {
            "group": "Multiple calls to getEvaluatedItems return the same instance",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "firstCallNotNull": true,
                        "secondCallNotNull": true,
                        "sameInstance": true
                    },
                    "description": "For a freshly created CollectorContext (default), invoke getEvaluatedItems() twice and assert that both returned references are identical (==) and non‑null."
                }
            ]
        },
        {
            "group": "Adding null element to an enabled collection succeeds",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "collectionEnabled": true,
                        "addResult": true,
                        "sizeAfterAdd": 1,
                        "containsNull": true
                    },
                    "description": "Create a CollectorContext where unevaluated items are enabled. Retrieve the collection via getEvaluatedItems() and call add(null). Verify that add returns true, the size becomes 1, and the collection reports that it contains a null element."
                }
            ]
        },
        {
            "group": "Adding null element to a disabled collection fails",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "collectionDisabled": true,
                        "addResult": false,
                        "sizeAfterAdd": 0,
                        "containsNull": false
                    },
                    "description": "Create a CollectorContext whose dynamic scope is constructed with disableUnevaluatedItems = true. Retrieve the collection via getEvaluatedItems() and attempt to add(null). Verify that add returns false, the size remains 0, and the collection does not contain null."
                }
            ]
        }
    ]
}