{
  "group": "BaseJsonValidator.atPath(String, int) - delegation to PathType.append",
  "cases": [
    {
      "input": [
        {
          "parameter": "currentPath",
          "value": "\"/a/b\""
        },
        {
          "parameter": "index",
          "value": "2"
        }
      ],
      "expected": "\"/a/b/2\"",
      "description": "When the validator is configured with PathType.JSON_POINTER, atPath should delegate to PathType.append which appends a '/' followed by the index. The resulting path must be '/a/b/2'."
    },
    {
      "input": [
        {
          "parameter": "currentPath",
          "value": "\"\""
        },
        {
          "parameter": "index",
          "value": "0"
        }
      ],
      "expected": "\"/0\"",
      "description": "Empty root path with PathType.JSON_POINTER should produce a leading '/' and the index. Verifies correct handling of an empty currentPath."
    },
    {
      "input": [
        {
          "parameter": "currentPath",
          "value": "\"$.store.book\""
        },
        {
          "parameter": "index",
          "value": "5"
        }
      ],
      "expected": "\"$.store.book[5]\"",
      "description": "When the validator uses PathType.JSON_PATH, atPath must produce a JSONPath expression by appending the index in square brackets. Expected result is '$.store.book[5]'."
    },
    {
      "input": [
        {
          "parameter": "currentPath",
          "value": "\"$\""
        },
        {
          "parameter": "index",
          "value": "1"
        }
      ],
      "expected": "\"$[1]\"",
      "description": "Root JSONPath '$' with an index should become '$[1]'. Tests the edge case where the currentPath is the root token."
    },
    {
      "input": [
        {
          "parameter": "currentPath",
          "value": "null"
        },
        {
          "parameter": "index",
          "value": "3"
        }
      ],
      "expected": "java.lang.NullPointerException",
      "description": "Passing a null currentPath must result in a NullPointerException because PathType.append expects a non‑null string."
    },
    {
      "input": [
        {
          "parameter": "currentPath",
          "value": "\"/some/path\""
        },
        {
          "parameter": "index",
          "value": "-1"
        }
      ],
      "expected": "\"/some/path/-1\"",
      "description": "Negative indexes are allowed; the method should simply append the string representation of the index. Confirms that no validation of the index value occurs inside atPath."
    },
    {
      "input": [
        {
          "parameter": "currentPath",
          "value": "\"/max/int\""
        },
        {
          "parameter": "index",
          "value": "2147483647"
        }
      ],
      "expected": "\"/max/int/2147483647\"",
      "description": "Maximum int value should be correctly converted to a string and appended. Tests handling of large numeric values."
    }
  ]
}