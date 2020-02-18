/*
 * Copyright (c) 2020 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */

import * as cp from "child_process";
import * as net from "net";
import { ExtensionContext, extensions, StatusBarAlignment, window, workspace } from "vscode";
import {
    LanguageClient,
    LanguageClientOptions,
    StreamInfo,
} from "vscode-languageclient/lib/main";
import { DefaultJavaVersionCheck } from "./JavaVersionCheck";

export async function activate(context: ExtensionContext) {
    const fs = require("fs");

    // path resolved to identify the location of the LSP server into the extension
    const extPath = extensions.getExtension("BroadcomMFD.cobol-language-support").extensionPath;
    const LSPServerPath = `${extPath}/server/lsp-service-cobol-0.10.1.jar`;

    try {
        await isJavaInstalled();
        if (!getLspPort() && !fs.existsSync(LSPServerPath)) {
            window.showErrorMessage("COBOL extension failed to start - LSP server not found");
            return;
        }
    } catch (err) {
        window.showErrorMessage(err.toString());
        return;
    }

    // Options to control the language client
    const clientOptions: LanguageClientOptions = {
        // Register the server for COBOL
        documentSelector: ["COBOL"],
    };
    const item = window.createStatusBarItem(StatusBarAlignment.Right, Number.MIN_VALUE);

    // Create the language client and start the client.
    const languageClient = new LanguageClient("COBOL", "LSP extension for COBOL language",
        createServerOptions(LSPServerPath), clientOptions);

    const disposable = languageClient.start();

    context.subscriptions.push(disposable);
    item.text = "Language Server is active";
}

async function isJavaInstalled() {
    return new Promise<any>((resolve, reject) => {
        const ls = cp.spawn("java", ["-version"]);
        ls.stderr.on("data", (data: any) => {
            const javaCheck = new DefaultJavaVersionCheck();
            if (!javaCheck.isJavaVersionSupported(data.toString())) {
                reject("Java version 8 expected");
            }
            resolve();
        });
        ls.on("error", (code: any) => {
            if ("Error: spawn java ENOENT" === code.toString()) {
                reject("Java 8 is not found");
            }
            reject(code);
        });
        ls.on("close", (code: number) => {
            if (code !== 0) {
                reject("An error occurred when checking if Java was installed");
            }
        });
    });
}

function getLspPort(): number | undefined {
    return +workspace.getConfiguration().get("broadcom-cobol-lsp.server.port");
}

function createServerOptions(jarPath: string) {
    const port = getLspPort();
    if (port) {
        // Connect to language server via socket
        const connectionInfo = {
            host: "localhost",
            port,
        };
        return () => {
            const socket = net.connect(connectionInfo);
            const result: StreamInfo = {
                reader: socket,
                writer: socket,
            };
            return Promise.resolve(result);
        };
    }
    return {
        args: ["-Dline.separator=\r\n", "-Xmx768M", "-jar", jarPath, "pipeEnabled"],
        command: "java",
        options: { stdio: "pipe", detached: false },
    };
}
