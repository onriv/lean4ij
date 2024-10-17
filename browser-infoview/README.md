# browser infoview
This is a frontend for using [infoview-app](https://github.com/leanprover/vscode-lean4/tree/master/lean4-infoview) inside a browser (, or it can be embedded in some browser environment). The  [infoview-app](https://github.com/leanprover/vscode-lean4/tree/master/lean4-infoview) project is already designed for running inside a browser. Here the infoview api defined in it is again bridge with a websocket api and make a connection to a http backend.

**There is only one websocket connection for both sending client requests and receiving server notifications** All other http endpoints are served for static resources.

The contract correctly is (TODO)
- send rpc requests with websocket data format like `'sendClientRequest', JSON.stringify(params)`
- received rpc responses with TODO
- received sever notification with TODO

The reason using websocket rather than sse is because there is a limit for a browser connecting to a same server, see: https://stackoverflow.com/questions/18584525/server-sent-events-and-browser-limits

# Development
For frontend proxy, create a raw `host-config.json` content, and run `runIDE`, the file will be replaced to the real proxy port (TODO some part should move out here) and, 
this should be more general usage.