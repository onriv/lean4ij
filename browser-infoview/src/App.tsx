/**
 * TODO refactor this
 */
import React, {useEffect, useRef} from 'react'
import './App.css'
import './vscode.css'
import './infoview.css'
import './Editor.css'
// TODO switch to loader
// import { loadRenderInfoview } from '@leanprover/infoview/loader'
import {renderInfoview} from '@leanprover/infoview'
import {EditorApi, InfoviewApi} from '@leanprover/infoview-api'
import {Rpc} from "./rpc.ts";

class WebSocketClient {
    private socket: WebSocket;
    private responseMap: Map<number, (response: any) => void>;
    private requestId: number;
    infoViewApi: InfoviewApi;

    constructor(url: string) {
        this.socket = new WebSocket(url);
        this.responseMap = new Map();
        this.requestId = 0;

        this.socket.addEventListener('message', (event) => {
            const resp  = JSON.parse(event.data)
            if (resp.method == 'updateTheme') {
                const style = document.querySelector('style')
                if (style) {
                    style.textContent = resp.data.theme
                }
            }
            if (resp.method == 'serverRestarted') {
                this.infoViewApi.serverRestarted(resp.data)
                return
            }
            if (resp.method == 'changedCursorLocation') {
                this.infoViewApi.changedCursorLocation(resp.data)
                return
            }
            if (resp.method == 'rpcResponse') {
                const callback = this.responseMap.get(resp.requestId);
                if (callback) {
                    this.responseMap.delete(resp.requestId);
                    callback(resp.data);
                }
            }
            if (resp.method == 'serverNotification') {
                this.infoViewApi.gotServerNotification(resp.data.method, resp.data.params)
            }
        });
    }

    sendMessage(method, message: any): Promise<any> {
        return new Promise((resolve) => {
            const id = this.requestId++;
            this.responseMap.set(id, resolve);
            // TODO all the frontend need better design
            this.socket.send(`${id},${method},${message}`);
        });
    }
}

export class DummyEditorApi implements EditorApi {
    private socket: WebSocketClient;

    constructor(socket: WebSocketClient) {
        this.socket = socket
    }

    async sendClientRequest(uri: string, method: string, params: any): Promise<any> {
        console.log(`Sending client request: ${method} for URI: ${uri}`);
        const resp = await this.socket.sendMessage('sendClientRequest', JSON.stringify(params))
        if (resp == undefined) {
            if (params.method == "Lean.Widget.getInteractiveGoals") {
                return undefined
            }
            if (params.method == "Lean.Widget.getInteractiveTermGoal") {
                // return {
                //     goals: [],
                //     hyps: []
                // }
                return undefined
            }
            return []
        }
        return resp
    }

    async sendClientNotification(uri: string, method: string, params: any): Promise<void> {
        console.log(`Sending client notification: ${method} for URI: ${uri}`);
        // Implement your logic here for sending notifications to the LSP server.
        // For example, log the notification or perform other actions.
    }

    async subscribeServerNotifications(method: string): Promise<void> {
        console.log(`Subscribing to server notifications: ${method}`);
        // Implement your logic here for subscribing to server notifications.
    }

    async unsubscribeServerNotifications(method: string): Promise<void> {
        console.log(`Unsubscribing from server notifications: ${method}`);
        // Implement your logic here for unsubscribing from server notifications.
    }

    async subscribeClientNotifications(method: string): Promise<void> {
        console.log(`Subscribing to client notifications: ${method}`);
        // Implement your logic here for subscribing to client notifications.
    }

    async unsubscribeClientNotifications(method: string): Promise<void> {
        console.log(`Unsubscribing from client notifications: ${method}`);
        // Implement your logic here for unsubscribing from client notifications.
    }

    async copyToClipboard(text: string): Promise<void> {
        console.log(`Copying text to clipboard: ${text}`);
        // Implement your logic here for copying text to the clipboard.
    }

    async insertText(text: string, kind: any/*TextInsertKind*/, pos?: any/*TextDocumentPositionParams*/): Promise<void> {
        console.log(`Inserting text: ${text} (kind: ${kind}) at position: ${JSON.stringify(pos)}`);
        // Implement your logic here for inserting text into a document.
    }

    async applyEdit(te: any/*WorkspaceEdit*/): Promise<void> {
        console.log(`Applying WorkspaceEdit: ${JSON.stringify(te)}`);
        // Implement your logic here for applying a WorkspaceEdit to the workspace.
    }

    async showDocument(show: /*ShowDocumentParams*/any): Promise<void> {
        console.log(`Showing document: ${JSON.stringify(show)}`);
        // Implement your logic here for showing a document in the editor.
    }

    async restartFile(uri: string): Promise<void> {
        console.log(`Restarting file: ${uri}`);
        // Implement your logic here for restarting a file.
    }

    async createRpcSession(uri: /*DocumentUri*/any): Promise<string> {
        console.log(`Creating RPC session for URI: ${uri}`);
        const resp = await this.socket.sendMessage('createRpcSession', JSON.stringify({
            uri: uri
        }))
        return resp
        // const res = await fetch('/api/createRpcSession', {
        //         method: "POST",
        //         headers: {
        //             "Content-Type": "application/json"
        //         },
        //         body: JSON.stringify({uri: uri})
        //     }
        // );
        // if (!res.ok) {
        //     throw new Error('Network response was not ok');
        // }
        // const result = await res.json();
        // return result.sessionId;
    }

    async closeRpcSession(sessionId: string): Promise<void> {
        console.log(`Closing RPC session: ${sessionId}`);
        // Implement your logic here for closing an RPC session.
    }
}


function App() {
    const div = useRef<HTMLDivElement>(null);
    const rpc = new Rpc((m: any) => {
        if (m.kind === 'initialize') {

        } else {
           console.log("send Message:" + m)
        }
    })
    useEffect(() => {
        // log
        console.log('loading infoview')
        // const imports = {
        //     '@leanprover/infoview': 'https://unpkg.com/@leanprover/infoview/dist/index.production.min.js',
        //     'react': 'https://unpkg.com/@leanprover/infoview/dist/react.production.min.js',
        //     'react/jsx-runtime': 'https://unpkg.com/@leanprover/infoview/dist/react-jsx-runtime.production.min.js',
        //     'react-dom': 'https://unpkg.com/@leanprover/infoview/dist/react-dom.production.min.js',
        // }
        // console.log(imports)
        // loadRenderInfoview(imports, [new DummyEditorApi(), div], ()=>{})
        window.addEventListener('message', e => rpc.messageReceived(e.data))
        // window.postMessage({hello: "world"}, '*')
        //
        // infoViewApi.serverRestarted()
        // I dont understand sse, hence this very poor impl...
        // const intervalId = setInterval(async () => {
        //     const res = await fetch('/api/serverInitialized');
        //     if (!res.ok) {
        //         throw new Error('Network response was not ok');
        //     }
        //     const result = await res.json();
        //     infoViewApi.serverRestarted(result)
        //     clearInterval(intervalId)
        //     // Handle the response here
        // }, 2000); // Sends the API request every 2 seconds
        // Create a new WebSocket connection

        const socketClient = new WebSocketClient('ws://' + location.host + '/ws')
        const infoViewApi: InfoviewApi = renderInfoview(new DummyEditorApi(socketClient), div.current!)
        socketClient.infoViewApi = infoViewApi

        // Connection opened
        // socket.addEventListener('open', (event) => {
        //     console.log('Connected to the WebSocket server');
        // });
        //
        // // Listen for messages
        // socket.addEventListener('message', (event) => {
        //     const resp  = JSON.parse(event.data)
        //     if (resp.method == 'serverRestarted') {
        //         infoViewApi.serverRestarted(resp.data)
        //         return
        //     }
        //     if (resp.method == 'changedCursorLocation') {
        //         infoViewApi.changedCursorLocation(resp.data)
        //         return
        //     }
        //     console.log('Message from server:', event.data);
        // });
        //
        // // Connection closed
        // socket.addEventListener('close', (event) => {
        //     console.log('Disconnected from the WebSocket server');
        // });
        //
        // // Handle errors
        // socket.addEventListener('error', (event) => {
        //     console.error('WebSocket error:', event);
        // });
        rpc.register(infoViewApi)

        //
        // // TODO this is temporally for sse bug
        // const serverRestarted = async () => {
        //     const res = await fetch('/api/serverRestarted');
        //     if (!res.ok) {
        //         throw new Error('Network response was not ok');
        //     }
        //     const result = await res.json();
        //     infoViewApi.serverRestarted(result)
        // }
        // const cursorEvent = async () => {
        //     while (true) {
        //         const res = await fetch('/api/poll');
        //         if (!res.ok) {
        //             throw new Error('Network response was not ok');
        //         }
        //         const resp = await res.json()
        //         const result = resp.data.data
        //         if (result.uri == undefined) {
        //             throw new Error('Network response was not ok: no uri in result');
        //         }
        //         infoViewApi.changedCursorLocation(result)
        //         // Handle the response here
        //     }
        // }
        // serverRestarted().then(r => {
        //     cursorEvent()
        // })


        // TODO temporally sse has bug
        // const source = new EventSource('/api/sse');
        // function logEvent(text) {
        //     console.log(text)
        // }
        // source.addEventListener('message', function(e) {
        //     logEvent('message:' + e.data);
        //     const data = JSON.parse(e.data)
        //     if (data.method == "serverInitialized") {
        //         (infoViewApi as any).serverRestarted(data.data)
        //         return
        //     }
        //     (infoViewApi as any).changedCursorLocation(data.data)
        // }, false);
        //
        // source.addEventListener('open', function(e) {
        //     logEvent('open');
        // }, false);
        //
        // source.addEventListener('error', function(e) {
        //     if ((e as any).readyState == EventSource.CLOSED) {
        //         logEvent('closed');
        //     } else {
        //         logEvent('error');
        //         console.log(e);
        //     }
        //     const res = e.srcElement
        // }, false);

        // I dont understand sse, hence this very poor impl...
        // const intervalId1 = setInterval(async () => {
        //     const res = await fetch('/api/changedCursorLocation');
        //     if (!res.ok) {
        //         throw new Error('Network response was not ok');
        //     }
        //     const result = await res.json();
        //     if (result.uri == undefined) {
        //         return
        //     }
        //     infoViewApi.changedCursorLocation(result)
        //     // Handle the response here
        // }, 1000); // Sends the API request every 2 seconds
    }, []);



    return <div ref={div} id="infoview"></div>;
}

export default App
