{
    "group": "detectVersionFromPath",
    "cases": [
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "nullPath"
                }
            ],
            "expected": "throws NullPointerException",
            "description": "The method is invoked with a null Path reference; the call to `path.spliterator()` should raise a NullPointerException because the implementation does not guard against a null argument."
        },
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "pathWithEmptyString"
                }
            ],
            "expected": "Optional.empty",
            "description": "The Path consists of a single element whose string representation is an empty string \"\". No entry for an empty key exists in `supportedVersions`, so the lookup yields null and the method returns an empty Optional."
        },
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "pathWithCaseMismatchSupported"
                }
            ],
            "expected": "Optional.empty",
            "description": "The Path contains an element \"DRAFT4\" (upper‑case) while the map stores the key in lower‑case (e.g., \"draft4\"). Because the lookup is case‑sensitive, the result is null and the method returns Optional.empty."
        },
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "pathWithManyUnsupportedThenSupported"
                }
            ],
            "expected": "Optional[VersionFlag.DRAFT_2020_12]",
            "description": "The Path has several unsupported elements (e.g., \"foo\", \"bar\", \"baz\") followed by a supported element \"draft2020-12\". After filtering out the null look‑ups, the first non‑null VersionFlag is returned, wrapped in an Optional."
        },
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "pathWithDuplicateSupported"
                }
            ],
            "expected": "Optional[VersionFlag.DRAFT_7]",
            "description": "The Path contains two identical supported elements (e.g., \"draft7\", \"draft7\"). Both map to the same VersionFlag; `findAny()` will return that flag, resulting in an Optional containing VersionFlag.DRAFT_7."
        },
        {
            "input": [
                {
                    "parameter": "path",
                    "value": "pathWithSupportedWhenMapCleared"
                }
            ],
            "expected": "Optional.empty",
            "description": "Before invoking the method the static `supportedVersions` map is cleared (e.g., via reflection). Even though the Path contains a string that would normally be supported (e.g., \"draft4\"), the lookup now yields null, so the method returns Optional.empty."
        }
    ]
}