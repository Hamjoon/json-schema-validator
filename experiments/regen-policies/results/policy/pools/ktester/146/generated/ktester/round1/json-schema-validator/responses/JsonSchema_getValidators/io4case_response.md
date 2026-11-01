{
    "tests": [
        {
            "group": "Validators Initialization - empty list",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "returnValue": "unmodifiable empty list",
                        "behavior": [
                            "read(JsonNode) is invoked exactly once",
                            "the returned list size is 0",
                            "any attempt to modify the list throws UnsupportedOperationException"
                        ]
                    },
                    "description": "When the private read() method returns an empty mutable list, getValidators() should wrap it with Collections.unmodifiableList, store it, and return an immutable empty list. The method must still call read() once and the result must be unmodifiable."
                }
            ]
        },
        {
            "group": "Validators Initialization - read throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "exception": "RuntimeException",
                        "behavior": [
                            "read(JsonNode) is invoked exactly once",
                            "the RuntimeException thrown by read propagates out of getValidators()",
                            "the internal validators field remains null"
                        ]
                    },
                    "description": "If the private read() method throws a RuntimeException (e.g., due to malformed schema), getValidators() must not catch it; the exception should bubble up to the caller and the validators cache must stay uninitialized."
                }
            ]
        },
        {
            "group": "Validators Caching - concurrent calls",
            "cases": [
                {
                    "input": [],
                    "expected": {
                        "returnValue": "same unmodifiable list instance for all threads",
                        "behavior": [
                            "read(JsonNode) is invoked exactly once despite multiple concurrent getValidators() calls",
                            "all threads receive the identical list reference (==)",
                            "the list is unmodifiable"
                        ]
                    },
                    "description": "When several threads invoke getValidators() at the same time on a freshly created JsonSchema (validators == null), the method must ensure that read() is executed only once and that each thread receives the same cached unmodifiable list. This tests the thread‑safety of the lazy‑initialization logic."
                }
            ]
        }
    ]
}