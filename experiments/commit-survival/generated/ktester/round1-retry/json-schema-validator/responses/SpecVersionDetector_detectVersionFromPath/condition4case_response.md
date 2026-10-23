{
  "group": "detectVersionFromPath",
  "cases": [
    {
      "input": [
        {
          "parameter": "path",
          "value": "emptyPath"
        }
      ],
      "expected": "Optional.empty",
      "description": "The supplied Path has no elements (spliterator is empty). The stream produces no strings, so the method must return an empty Optional."
    },
    {
      "input": [
        {
          "parameter": "path",
          "value": "pathWithUnsupported"
        }
      ],
      "expected": "Optional.empty",
      "description": "The Path contains a single element whose string representation is not present in `supportedVersions`. After the map lookup the value is null, filtered out, and the method returns an empty Optional."
    },
    {
      "input": [
        {
          "parameter": "path",
          "value": "pathWithSupported"
        }
      ],
      "expected": "Optional[VersionFlag.DRAFT_4]",
      "description": "The Path contains one element whose string matches a key in `supportedVersions` (e.g., \"draft4\" → VersionFlag.DRAFT_4). The lookup yields a non‑null VersionFlag, which passes the filter and is returned wrapped in an Optional."
    },
    {
      "input": [
        {
          "parameter": "path",
          "value": "pathMixedSupportedFirstUnsupportedSecond"
        }
      ],
      "expected": "Optional[VersionFlag.DRAFT_7]",
      "description": "The Path contains two elements: the first maps to null (unsupported), the second maps to a supported version (e.g., \"draft7\" → VersionFlag.DRAFT_7). The stream skips the null and returns the first non‑null VersionFlag wrapped in an Optional."
    },
    {
      "input": [
        {
          "parameter": "path",
          "value": "pathWithMultipleSupported"
        }
      ],
      "expected": "Optional[VersionFlag.DRAFT_4] or Optional[VersionFlag.DRAFT_7]",
      "description": "The Path contains several elements, all of which map to supported versions (e.g., \"draft4\" and \"draft7\"). The method uses `findAny()`, so any one of the matching VersionFlag values may be returned. The test verifies that the result is present and belongs to the set of expected supported flags."
    },
    {
      "input": [
        {
          "parameter": "path",
          "value": "pathWithNullElement"
        }
      ],
      "expected": "throws NullPointerException",
      "description": "If the Path's spliterator yields a null element, the `Path::toString` call will throw a NullPointerException. This case confirms the method does not guard against null entries in the Path."
    }
  ]
}