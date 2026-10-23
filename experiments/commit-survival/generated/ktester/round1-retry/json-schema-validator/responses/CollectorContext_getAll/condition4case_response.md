{
  "group": "CollectorContext.getAll() - merging behavior",
  "cases": [
    {
      "input": [],
      "expected": "{}",
      "description": "Both collectorMap and collectorLoadMap are empty. The returned map should be empty."
    },
    {
      "input": [],
      "expected": "{\"a\":1,\"b\":2}",
      "description": "collectorMap contains two entries (a=1, b=2) and collectorLoadMap is empty. The returned map should contain exactly the entries from collectorMap."
    },
    {
      "input": [],
      "expected": "{\"x\":\"foo\",\"y\":\"bar\"}",
      "description": "collectorMap is empty while collectorLoadMap contains two entries (x=\"foo\", y=\"bar\"). The returned map should contain exactly the entries from collectorLoadMap."
    },
    {
      "input": [],
      "expected": "{\"k1\":10,\"k2\":20,\"k3\":30}",
      "description": "Both maps have distinct keys (collectorMap: k1=10, k2=20; collectorLoadMap: k3=30). The returned map should contain all three entries."
    },
    {
      "input": [],
      "expected": "{\"dup\":\"load\",\"unique\":\"collector\"}",
      "description": "Both maps contain the same key \"dup\" with different values (collectorMap: \"collector\", collectorLoadMap: \"load\") and collectorMap also has a unique key \"unique\". The returned map should have \"dup\" value from collectorLoadMap (\"load\") and retain the unique entry."
    },
    {
      "input": [],
      "expected": "Returned map is independent; modifications to it do not affect internal collectorMap or collectorLoadMap.",
      "description": "After calling getAll(), add a new entry to the returned map and verify that the internal collectorMap and collectorLoadMap remain unchanged."
    }
  ]
}