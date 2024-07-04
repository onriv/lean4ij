import React, { useState, useEffect, useRef } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import { loadRenderInfoview } from '@leanprover/infoview/loader'
import { renderInfoview } from '@leanprover/infoview'
import { InfoviewApi, EditorApi } from '@leanprover/infoview-api'
import {Rpc} from "./rpc.ts";

class ServerEventSource {
    private source: EventSource;
    private eventsUl: HTMLUListElement;

    constructor(apiUrl: string, eventsUlId: string) {
        this.source = new EventSource(apiUrl);
        this.eventsUl = document.getElementById(eventsUlId) as HTMLUListElement;

        this.source.addEventListener('message', this.handleMessage.bind(this), false);
        this.source.addEventListener('open', this.handleOpen.bind(this), false);
        this.source.addEventListener('error', this.handleError.bind(this), false);
    }

    private logEvent(text: string) {
        // const li = document.createElement('li');
        // li.innerText = text;
        // this.eventsUl.appendChild(li);
    }

    private handleMessage(e: MessageEvent) {
        this.logEvent('message:' + e.data);
    }

    private handleOpen(e: Event) {
        this.logEvent('open');
    }

    private handleError(e: Event) {
        if (e.readyState === EventSource.CLOSED) {
            this.logEvent('closed');
        } else {
            this.logEvent('error');
            console.log(e);
        }
    }
}

export class DummyEditorApi implements EditorApi {
    async sendClientRequest(uri: string, method: string, params: any): Promise<any> {
        console.log(`Sending client request: ${method} for URI: ${uri}`);
        const res = await fetch('/api/sendClientRequest', {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(params)
            }
        );
        if (!res.ok) {
            throw new Error('Network response was not ok');
        }
        const result = await res.json();
        return result;
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

    async insertText(text: string, kind: TextInsertKind, pos?: TextDocumentPositionParams): Promise<void> {
        console.log(`Inserting text: ${text} (kind: ${kind}) at position: ${JSON.stringify(pos)}`);
        // Implement your logic here for inserting text into a document.
    }

    async applyEdit(te: WorkspaceEdit): Promise<void> {
        console.log(`Applying WorkspaceEdit: ${JSON.stringify(te)}`);
        // Implement your logic here for applying a WorkspaceEdit to the workspace.
    }

    async showDocument(show: ShowDocumentParams): Promise<void> {
        console.log(`Showing document: ${JSON.stringify(show)}`);
        // Implement your logic here for showing a document in the editor.
    }

    async restartFile(uri: string): Promise<void> {
        console.log(`Restarting file: ${uri}`);
        // Implement your logic here for restarting a file.
    }

    async createRpcSession(uri: DocumentUri): Promise<string> {
        console.log(`Creating RPC session for URI: ${uri}`);
        const res = await fetch('/api/createRpcSession', {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({uri: uri})
            }
        );
        if (!res.ok) {
            throw new Error('Network response was not ok');
        }
        const result = await res.json();
        return result.session;
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
        const infoViewApi: InfoviewApi = renderInfoview(new DummyEditorApi(), div.current!)
        rpc.register(infoViewApi)
        // window.postMessage({hello: "world"}, '*')
        //
        // infoViewApi.serverRestarted()
        // I dont understand sse, hence this very poor impl...
        const intervalId = setInterval(async () => {
            const res = await fetch('/api/serverInitialized');
            if (!res.ok) {
                throw new Error('Network response was not ok');
            }
            const result = await res.json();
            infoViewApi.serverRestarted(result)
            clearInterval(intervalId)
            // Handle the response here
        }, 2000); // Sends the API request every 2 seconds

        // I dont understand sse, hence this very poor impl...
        const intervalId1 = setInterval(async () => {
            const res = await fetch('/api/changedCursorLocation');
            if (!res.ok) {
                throw new Error('Network response was not ok');
            }
            const result = await res.json();
            if (result.uri == undefined) {
                return
            }
            infoViewApi.changedCursorLocation(result)
            // Handle the response here
        }, 40); // Sends the API request every 2 seconds
    }, []);
    //sconst myEventSource = new ServerEventSource('/api/events', 'events');



    return <div ref={div}></div>;
}

export default App