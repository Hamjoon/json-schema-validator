{
    "testCases": [
        {
            "group": "Preload propagates NullPointerException from null schema element",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Create an AnyOfValidator whose schemas list contains a null entry. When preloadJsonSchema() invokes preloadJsonSchemas(this.schemas), the internal iteration over the list attempts to call a method on the null schema, causing a NullPointerException which must be propagated out of preloadJsonSchema()."
                }
            ]
        },
        {
            "group": "Preload propagates exception from schemas list access (UnsupportedOperationException)",
            "cases": [
                {
                    "input": [],
                    "expected": "UnsupportedOperationException",
                    "description": "Supply an AnyOfValidator with a custom List implementation whose size() method throws UnsupportedOperationException. The call to preloadJsonSchemas(this.schemas) will trigger the size() call before any schema processing, causing the exception to be thrown and propagated."
                }
            ]
        },
        {
            "group": "Preload propagates Error thrown by preloadJsonSchemas (AssertionError)",
            "cases": [
                {
                    "input": [],
                    "expected": "AssertionError",
                    "description": "Subclass AnyOfValidator and override preloadJsonSchemas(List<JsonSchema>) to throw an AssertionError. Verify that preloadJsonSchema() does not catch the Error and that the AssertionError propagates to the caller while canShortCircuit() is not invoked."
                }
            ]
        }
    ]
}