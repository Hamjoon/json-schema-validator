{
    "group": "detectVersionFromPath",
    "cases": [
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "pathWhenMapNull"
                }
            ],
            "expected": "throws NullPointerException",
            "description": "The static `supportedVersions` map is set to null (e.g., via reflection) before invoking the method. When the stream reaches the `supportedVersions::get` mapping step, a NullPointerException is thrown because the map reference is null."
        },
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "pathWithSpliteratorException"
                }
            ],
            "expected": "throws RuntimeException",
            "description": "The supplied Path returns a custom Spliterator whose `tryAdvance` (or `forEachRemaining`) throws a RuntimeException during stream traversal. The exception propagates out of the stream pipeline, causing the method to throw that RuntimeException."
        }
    ]
}