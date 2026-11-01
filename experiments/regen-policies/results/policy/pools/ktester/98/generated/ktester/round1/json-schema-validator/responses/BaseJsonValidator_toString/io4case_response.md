{
  "group": "BaseJsonValidator.toString() - additional cases",
  "cases": [
    {
      "input": [],
      "expected": "\"\"",
      "description": "When the evaluation path contains a single element that is an empty string, getEvaluationPath().getName(-1) returns \"\" and toString() must return the empty string."
    },
    {
      "input": [],
      "expected": "\"   \"",
      "description": "When the evaluation path's last element consists only of whitespace, toString() should return that whitespace unchanged."
    },
    {
      "input": [],
      "expected": "\"12345\"",
      "description": "When the last element of the evaluation path is a numeric string, toString() must return the exact numeric string."
    },
    {
      "input": [],
      "expected": "\"node@#%&*\"",
      "description": "When the evaluation path ends with a string containing special characters, toString() should return the same special‑character string."
    },
    {
      "input": [],
      "expected": "NullPointerException",
      "description": "If getEvaluationPath() returns null, invoking getName(-1) causes a NullPointerException which toString() must propagate."
    },
    {
      "input": [],
      "expected": "IllegalStateException",
      "description": "When the JsonNodePath implementation throws an IllegalStateException inside getName(-1), toString() should propagate that exception."
    },
    {
      "input": [],
      "expected": "\"leaf1000\"",
      "description": "When the evaluation path contains a large number of elements (e.g., 1000) and the last element is \"leaf1000\", toString() must correctly return the last element regardless of path length."
    }
  ]
}