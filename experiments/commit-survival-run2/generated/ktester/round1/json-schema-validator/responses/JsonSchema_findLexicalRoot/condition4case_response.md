{
  "group": "findLexicalRoot",
  "cases": [
    {
      "input": [
        {
          "parameter": "schemaInstance",
          "value": "rootSchema(hasId=true, parent=null)"
        }
      ],
      "expected": "returns the same instance (rootSchema)",
      "description": "The current schema already has an ID, so the while‑loop is never entered and the method should return the invoking object."
    },
    {
      "input": [
        {
          "parameter": "schemaInstance",
          "value": "leafSchema(hasId=false, parent=null)"
        }
      ],
      "expected": "returns the same instance (leafSchema)",
      "description": "The schema has no ID and also has no parent. The loop breaks immediately and the method returns the original instance."
    },
    {
      "input": [
        {
          "parameter": "schemaInstance",
          "value": "childSchema(hasId=false, parent=parentSchema(hasId=true, parent=null))"
        }
      ],
      "expected": "returns parentSchema",
      "description": "The child does not have an ID, but its direct parent does. The loop iterates once, finds the parent with hasId=true and returns it."
    },
    {
      "input": [
        {
          "parameter": "schemaInstance",
          "value": "grandChild(hasId=false, parent=midSchema(hasId=false, parent=rootSchema(hasId=true, parent=null)))"
        }
      ],
      "expected": "returns rootSchema",
      "description": "Neither the grand‑child nor its immediate parent have an ID. The loop walks up two levels until it reaches the ancestor that has hasId=true and returns that ancestor."
    },
    {
      "input": [
        {
          "parameter": "schemaInstance",
          "value": "deepLeaf(hasId=false, parent=level2(hasId=false, parent=level1(hasId=false, parent=null)))"
        }
      ],
      "expected": "returns level1 (the highest ancestor with parent=null)",
      "description": "All ancestors lack an ID. The loop continues until it reaches the topmost schema whose parent is null, then breaks and returns that topmost schema."
    }
  ]
}