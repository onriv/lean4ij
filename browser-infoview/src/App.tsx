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

// for right click
// copy from https://fkhadra.github.io/react-contexify/quick-start/
import {
    Menu,
    Item,
    Separator,
    Submenu,
    useContextMenu
} from "react-contexify";

import "react-contexify/dist/ReactContexify.css";
const MENU_ID = "menu-id";


/**
 * `RpcNeedsReconnect` from `@leanprover/infoview`'s `RpcErrorCode`: the infoview treats a request failing with it as
 * "recreate the RPC session and retry" instead of showing an error, which is exactly what a dropped websocket needs.
 */
const RPC_NEEDS_RECONNECT = -32900

const RECONNECT_DELAY_INITIAL_MS = 500
const RECONNECT_DELAY_MAX_MS = 10_000

/**
 * Websocket to the ktor server inside the IDE. The connection can drop while the page stays alive (observed after
 * the laptop slept with the lid closed: the JCEF tool window kept showing the last goal and never updated again,
 * while a freshly opened browser tab worked), so the client reconnects with backoff. On every (re)connection the
 * server replays `updateTheme`, `serverRestarted` and the last cursor location, which makes the infoview drop its RPC
 * sessions and re-render the current position.
 */
class WebSocketClient {
    private socket: WebSocket | undefined;
    private responseMap: Map<number, (response: any) => void>;
    private requestId: number;
    private reconnectDelayMs = RECONNECT_DELAY_INITIAL_MS;
    infoViewApi: InfoviewApi;

    constructor(private readonly url: string) {
        this.responseMap = new Map();
        this.requestId = 0;
        this.connect();
    }

    private connect() {
        const socket = new WebSocket(this.url);
        this.socket = socket;
        socket.addEventListener('open', () => {
            console.log('infoview websocket connected');
            this.reconnectDelayMs = RECONNECT_DELAY_INITIAL_MS;
        });
        socket.addEventListener('message', (event) => this.onMessage(event));
        socket.addEventListener('close', (event) => this.onClose(socket, event));
    }

    private onClose(socket: WebSocket, event: CloseEvent) {
        if (socket !== this.socket) {
            return;
        }
        console.warn(`infoview websocket closed (code ${event.code}), reconnecting in ${this.reconnectDelayMs}ms`);
        this.socket = undefined;
        // nobody will answer the requests that were in flight on the old connection; fail them so the infoview retries
        const pending = Array.from(this.responseMap.values());
        this.responseMap.clear();
        for (const callback of pending) {
            callback({error: {code: RPC_NEEDS_RECONNECT, message: 'infoview websocket closed'}});
        }
        setTimeout(() => this.connect(), this.reconnectDelayMs);
        this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, RECONNECT_DELAY_MAX_MS);
    }

    private onMessage(event: MessageEvent) {
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
                callback(resp);
            }
        }
        if (resp.method == 'serverNotification') {
            this.infoViewApi.gotServerNotification(resp.data.method, resp.data.params)
        }
    }

    sendMessage(method, message: any): Promise<any> {
        return new Promise((resolve, reject) => {
            const socket = this.socket;
            if (socket === undefined || socket.readyState !== WebSocket.OPEN) {
                reject({code: RPC_NEEDS_RECONNECT, message: 'infoview websocket is not connected'});
                return;
            }
            const id = this.requestId++;
            // the server answers every request either with `data` or with an RPC `error` ({code, message});
            // rejecting with the error lets the infoview show it and apply its own retry logic
            this.responseMap.set(id, (resp) => resp.error ? reject(resp.error) : resolve(resp.data));
            // TODO all the frontend need better design
            socket.send(`${id},${method},${message}`);
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
        debugger
    }

    async subscribeServerNotifications(method: string): Promise<void> {
        console.log(`Subscribing to server notifications: ${method}`);
        // Implement your logic here for subscribing to server notifications.
        debugger
    }

    async unsubscribeServerNotifications(method: string): Promise<void> {
        console.log(`Unsubscribing from server notifications: ${method}`);
        // Implement your logic here for unsubscribing from server notifications.
        debugger
    }

    async subscribeClientNotifications(method: string): Promise<void> {
        console.log(`Subscribing to client notifications: ${method}`);
        // Implement your logic here for subscribing to client notifications.
        debugger
    }

    async unsubscribeClientNotifications(method: string): Promise<void> {
        console.log(`Unsubscribing from client notifications: ${method}`);
        // Implement your logic here for unsubscribing from client notifications.
        debugger
    }

    async copyToClipboard(text: string): Promise<void> {
        console.log(`Copying text to clipboard: ${text}`);
        // Implement your logic here for copying text to the clipboard.
        debugger
    }

    async insertText(text: string, kind: any/*TextInsertKind*/, pos?: any/*TextDocumentPositionParams*/): Promise<void> {
        console.log(`Inserting text: ${text} (kind: ${kind}) at position: ${JSON.stringify(pos)}`);
        // Implement your logic here for inserting text into a document.
        debugger
    }

    async applyEdit(te: any/*WorkspaceEdit*/): Promise<void> {
        console.log(`Applying WorkspaceEdit: ${JSON.stringify(te)}`);
        // Implement your logic here for applying a WorkspaceEdit to the workspace.
        return await this.socket.sendMessage('applyEdit', JSON.stringify(te))
    }

    async showDocument(show: /*ShowDocumentParams*/any): Promise<void> {
        console.log(`Showing document: ${JSON.stringify(show)}`);
        // Implement your logic here for showing a document in the editor.
        debugger
    }

    async restartFile(uri: string): Promise<void> {
        console.log(`Restarting file: ${uri}`);
        // Implement your logic here for restarting a file.
        debugger
    }

    async createRpcSession(uri: /*DocumentUri*/any): Promise<string> {
        console.log(`Creating RPC session for URI: ${uri}`);
        const resp = await this.socket.sendMessage('createRpcSession', JSON.stringify({
            uri: uri
        }))
        return resp
    }

    async closeRpcSession(sessionId: string): Promise<void> {
        console.log(`Closing RPC session: ${sessionId}`);
        // Implement your logic here for closing an RPC session.
        debugger
    }
}

function loadInfoview(div : HTMLDivElement) {
    // log
    console.log('loading infoview')
    const host = `http://${location.host}/imports`
    const imports = {
        '@leanprover/infoview': `${host}/index.production.min.js`,
        'react': `${host}/react.production.min.js`,
        'react/jsx-runtime': `${host}/react-jsx-runtime.production.min.js`,
        'react-dom': `${host}/react-dom.production.min.js`,
    }
    const editorApi = new WebSocketEditorApi();
}

function App() {
    // copy from https://fkhadra.github.io/react-contexify/quick-start/
    // 🔥 you can use this hook from everywhere. All you need is the menu id
    const { show } = useContextMenu({
        id: MENU_ID
    });

    function copyItem({ event, props, triggerEvent, data }){
        console.log(event, props, triggerEvent, data );
    }

    function isItemDisabled({ props, data, triggerEvent }) {
        return triggerEvent.srcElement.getAttribute("data-vscode-context") == null
    }

    function gotoDefinition({ event, props, triggerEvent, data }){
        const contextStr : string = triggerEvent.srcElement.getAttribute("data-vscode-context")
        const context = JSON.parse(contextStr)
        const api = props as InfoviewApi
        api.goToDefinition(context.interactiveCodeTagId)
    }

    const div = useRef<HTMLDivElement>(null);

    useEffect(() => {
        // log
        console.log('loading infoview')
        const host = `http://${location.host}/imports`
        /**
         * The imports are documented in
         * https://github.com/leanprover/vscode-lean4/blob/master/lean4-infoview/src/loader.ts
         * and the code used in vscode also give some hints:
         * https://github.com/leanprover/vscode-lean4/blob/master/vscode-lean4/webview/index.ts
         * old version of lean4web also has it, but currently it's gone, weird
         * check https://github.com/leanprover-community/lean4web/blob/1ed09cf5521421b7f2107220770c35d44db58ef8/client/src/Editor.tsx#L137
         * for the usage in lean4web
         */
        const imports = {
            '@leanprover/infoview': `${host}/index.production.min.js`,
            'react': `${host}/react.production.min.js`,
            'react/jsx-runtime': `${host}/react-jsx-runtime.production.min.js`,
            'react-dom': `${host}/react-dom.production.min.js`,
        }
        const editorApi = new WebSocketEditorApi();
        loadRenderInfoview(imports, [editorApi, div.current], (api: InfoviewApi)=> {
            editorApi.registerInfoApi(api)
            div.current.oncontextmenu = (ev: MouseEvent) => {
                show({
                    event:ev,
                    props: api
                })
            }

        })
    }, [show])
    return <div>
        <div ref={div} id="infoview"></div>
        <Menu id={MENU_ID}>
            <Item onClick={gotoDefinition} disabled={isItemDisabled}>
                Go to definition
            </Item>
        </Menu>
    </div>
}

export default App
