{
    "group": "loadCollectors - concurrent modification during collection",
    "cases": [
        {
            "input": [],
            "expected": "ConcurrentModificationException should be thrown when a Collector's collect() method modifies collectorMap during loadCollectors execution",
            "description": "Tests that loadCollectors propagates ConcurrentModificationException if a Collector alters collectorMap (e.g., adds a new entry) while iteration is in progress."
        }
    ]
}