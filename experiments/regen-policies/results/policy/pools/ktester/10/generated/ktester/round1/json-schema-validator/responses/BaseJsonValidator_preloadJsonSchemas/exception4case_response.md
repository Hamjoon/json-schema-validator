{
    "group": "preloadJsonSchemas concurrency exceptions",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "modifyingCollection"
                }
            ],
            "expected": "ConcurrentModificationException is thrown when a schema's initializeValidators() method modifies the same collection being iterated, causing the for‑each loop to detect concurrent modification.",
            "description": "Validate that the method does not swallow ConcurrentModificationException caused by a schema adding/removing elements from the collection during iteration."
        },
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "iteratorThrowsCollection"
                }
            ],
            "expected": "RuntimeException (e.g., IllegalStateException) thrown by the collection's iterator() method propagates out of preloadJsonSchemas.",
            "description": "Confirm that if the collection's iterator() implementation throws an unchecked exception when the for‑each loop attempts to obtain the iterator, the exception bubbles up unchanged."
        },
        {
            "input": [
                {
                    "parameter": "schemas",
                    "value": "nullIteratorCollection"
                }
            ],
            "expected": "NullPointerException is thrown because the collection's iterator() method returns null, leading the for‑each construct to dereference a null iterator.",
            "description": "Ensure that a collection returning a null iterator causes a NullPointerException during iteration, demonstrating that the method does not guard against malformed iterables."
        }
    ]
}