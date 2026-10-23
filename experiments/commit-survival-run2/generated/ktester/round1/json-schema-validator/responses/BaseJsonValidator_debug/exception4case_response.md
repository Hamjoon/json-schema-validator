{
    "debugMethodExceptionScenarios": [
        {
            "group": "debugMethodExceptionScenarios",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mockLogger10"
                        },
                        {
                            "parameter": "node",
                            "value": "exceptionThrowingNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "jsonRootNodeJ"
                        },
                        {
                            "parameter": "at",
                            "value": "/exception/node"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "The JsonNode implementation throws a RuntimeException from its toString() method; SLF4J evaluates the argument during formatting, so the exception propagates out of debug()."
                },
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mockLogger11"
                        },
                        {
                            "parameter": "node",
                            "value": "jsonNodeK"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "exceptionThrowingRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "/exception/root"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "The rootNode's toString() throws a RuntimeException; the formatter invokes toString on each argument, causing the exception to be thrown from debug()."
                },
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "mockLogger12"
                        },
                        {
                            "parameter": "node",
                            "value": "exceptionThrowingNode"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "exceptionThrowingRootNode"
                        },
                        {
                            "parameter": "at",
                            "value": "/both/exception"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "Both node and rootNode throw RuntimeException from toString(); the first exception encountered during argument evaluation is propagated."
                },
                {
                    "input": [
                        {
                            "parameter": "logger",
                            "value": "errorThrowingLogger"
                        },
                        {
                            "parameter": "node",
                            "value": "jsonNodeL"
                        },
                        {
                            "parameter": "rootNode",
                            "value": "jsonRootNodeL"
                        },
                        {
                            "parameter": "at",
                            "value": "/error/logger"
                        }
                    ],
                    "expected": "AssertionError",
                    "description": "Logger.debug is stubbed to throw an AssertionError (a subclass of Error); the method does not catch Errors, so the AssertionError propagates unchanged."
                }
            ]
        }
    ]
}