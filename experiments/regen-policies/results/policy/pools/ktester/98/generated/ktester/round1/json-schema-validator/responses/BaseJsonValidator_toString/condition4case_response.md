{
  "group": "BaseJsonValidator.toString()",
  "cases": [
    {
      "input": [],
      "expected": "\"root\"",
      "description": "When the evaluation path contains a single element \"root\", getEvaluationPath().getName(-1) should return that element. toString() must therefore return \"root\"."
    },
    {
      "input": [],
      "expected": "\"grandchild\"",
      "description": "When the evaluation path contains multiple elements [\"root\", \"child\", \"grandchild\"], getName(-1) should return the last element \"grandchild\". toString() must return \"grandchild\"."
    },
    {
      "input": [],
      "expected": "IndexOutOfBoundsException",
      "description": "When the evaluation path is empty (no elements), calling getName(-1) is expected to throw an IndexOutOfBoundsException. toString() should propagate this exception."
    },
    {
      "input": [],
      "expected": "null",
      "description": "When the evaluation path’s last element is explicitly null, getName(-1) returns null. toString() should therefore return null."
    }
  ]
}