{
  "testCases": [
    {
      "group": "BaseJsonValidator.atPath(String, int) - JSON_POINTER edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"/\""
            },
            {
              "parameter": "index",
              "value": "0"
            }
          ],
          "expected": "\"/0\"",
          "description": "Root path consisting of a single slash should still produce a single leading slash followed by the index, avoiding a double slash."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"/a/b/\""
            },
            {
              "parameter": "index",
              "value": "5"
            }
          ],
          "expected": "\"/a/b/5\"",
          "description": "When the currentPath already ends with a slash, atPath must not introduce a double slash; it should simply append the index."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"/a~b\""
            },
            {
              "parameter": "index",
              "value": "1"
            }
          ],
          "expected": "\"/a~b/1\"",
          "description": "The method must treat '~' as a normal character and correctly append the index."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"/a b\""
            },
            {
              "parameter": "index",
              "value": "2"
            }
          ],
          "expected": "\"/a b/2\"",
          "description": "Spaces inside the path segment must be preserved when the index is appended."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"/max/int\""
            },
            {
              "parameter": "index",
              "value": "-2147483648"
            }
          ],
          "expected": "\"/max/int/-2147483648\"",
          "description": "Appending Integer.MIN_VALUE verifies that extreme negative values are correctly converted to a string and appended."
        }
      ]
    },
    {
      "group": "BaseJsonValidator.atPath(String, int) - JSON_PATH edge cases",
      "cases": [
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"$.store.book\""
            },
            {
              "parameter": "index",
              "value": "0"
            }
          ],
          "expected": "\"$.store.book[0]\"",
          "description": "Appending index 0 to a typical JSONPath array token should produce the expected bracket notation."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"$\""
            },
            {
              "parameter": "index",
              "value": "0"
            }
          ],
          "expected": "\"$[0]\"",
          "description": "Root JSONPath token with index 0 must result in '$[0]'. This complements the existing test for index 1."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"$.array[2]\""
            },
            {
              "parameter": "index",
              "value": "3"
            }
          ],
          "expected": "\"$.array[2][3]\"",
          "description": "When the currentPath already ends with an array index, atPath should append another bracketed index, yielding a chained array access."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"$.complex.property\""
            },
            {
              "parameter": "index",
              "value": "-5"
            }
          ],
          "expected": "\"$.complex.property[-5]\"",
          "description": "Negative indexes are allowed in JSONPath; the method must include the minus sign inside the brackets."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"$.unicode_π\""
            },
            {
              "parameter": "index",
              "value": "1"
            }
          ],
          "expected": "\"$.unicode_π[1]\"",
          "description": "Unicode characters in the path token must be preserved when the index is appended."
        },
        {
          "input": [
            {
              "parameter": "currentPath",
              "value": "\"\""
            },
            {
              "parameter": "index",
              "value": "4"
            }
          ],
          "expected": "\"[4]\"",
          "description": "An empty currentPath for JSON_PATH should result in a bare bracketed index, confirming that the method does not prepend a dot or other character."
        }
      ]
    }
  ]
}