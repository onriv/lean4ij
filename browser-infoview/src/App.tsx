/**
 * TODO refactor this
 */
import React, {useEffect, useRef} from 'react'
import './App.css'
import './vscode.css'
import './infoview.css'
import './Editor.css'
import {EditorApi, InfoviewApi} from '@leanprover/infoview-api'
import {loadRenderInfoview} from "@leanprover/infoview/loader";

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

/**
 * An editor api communicating to the editor via websocket
 */
export class WebSocketEditorApi implements EditorApi {
    private socket: WebSocketClient;

    registerInfoApi(api: InfoviewApi) {
        const socketClient = new WebSocketClient('ws://' + location.host + '/ws')
        socketClient.infoViewApi = api
        this.socket = socketClient
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

/**
 * The imports are documented in
 * https://github.com/leanprover/vscode-lean4/blob/master/lean4-infoview/src/loader.ts
 * and the code used in vscode also give some hints:
 * https://github.com/leanprover/vscode-lean4/blob/master/vscode-lean4/webview/index.ts
 * old version of lean4web also has it, but currently it's gone, weird
 * check https://github.com/leanprover-community/lean4web/blob/1ed09cf5521421b7f2107220770c35d44db58ef8/client/src/Editor.tsx#L137
 * for the usage in lean4web
 */
function loadInfoview(div : HTMLDivElement) {
    // log
    console.log('loading infoview')

    const imports = {
        '@leanprover/infoview': 'http://'+location.host+'/imports/index.production.min.js',
        'react': 'http://'+location.host+'/imports/react.production.min.js',
        'react/jsx-runtime': 'http://'+location.host+'/imports/react-jsx-runtime.production.min.js',
        'react-dom': 'http://'+location.host+'/imports/react-dom.production.min.js',
    }
    const editorApi = new WebSocketEditorApi();
    loadRenderInfoview(imports, [editorApi, div], (api: InfoviewApi)=> editorApi.registerInfoApi(api))
}

function App() {
    const div = useRef<HTMLDivElement>(null);
    useEffect(() => loadInfoview(div.current), []);
    return <div ref={div} id="infoview"></div>;
}

export default App
