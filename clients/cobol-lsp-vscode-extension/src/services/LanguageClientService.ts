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

import * as fs from "fs";
import * as net from "net";
import * as vscode from "vscode";
import { LANGUAGE_ID, PATHS_ZOWE, SETTINGS_SECTION } from "../constants";
import { CancellationToken, ConfigurationParams, ConfigurationRequest, LanguageClient, LanguageClientOptions, StreamInfo} from "vscode-languageclient";
import { ConfigurationWorkspaceMiddleware } from "vscode-languageclient/lib/configuration";
import { CopybooksPathGenerator } from "./CopybooksPathGenerator";
import { JavaCheck } from "./JavaCheck";
import { Prioritizer } from "./Prioritizer";

export class LanguageClientService {
    private jarPath: string;

    constructor(private copybooksPathGenerator: CopybooksPathGenerator, private prioritizer: Prioritizer) {
        const ext = vscode.extensions.getExtension("BroadcomMFD.cobol-language-support");
        this.jarPath = `${ext.extensionPath}/server/lsp-service-cobol-${ext.packageJSON.version}.jar`;
    }
    async checkPrerequisites(): Promise<void> {
        await new JavaCheck().isJavaInstalled();
        if (!this.getLspPort() && !fs.existsSync(this.jarPath)) {
            throw new Error("LSP server for " + LANGUAGE_ID + " not found");
        }
    }
    start(): vscode.Disposable {
        const languageClient = new LanguageClient(LANGUAGE_ID,
            "LSP extension for " + LANGUAGE_ID + " language",
            this.createServerOptions(this.jarPath),
            this.createClientOptions());
        return languageClient.start();
    }

    private getLspPort(): number | undefined {
        return +vscode.workspace.getConfiguration().get("broadcom-cobol-lsp.server.port");
    }

    private createClientOptions(): LanguageClientOptions {

        //TODO: Define the middleware in a separate structure injected into the language client...
        const signatureFunc: ConfigurationRequest.MiddlewareSignature = async (
            params: ConfigurationParams,
            token: CancellationToken,
            next: ConfigurationRequest.HandlerSignature) => {

            // TODO if request params are right

            // TODO after server modification this method return the local copybooks URI
            /*
             there should be logic here to check if user complete the zowe settings, than add
             calculated path to response object
             use checkMFSettings and this.copybooksPathGenerator.listUris()
            */

            /**
             * TODO
             * at initialize moment, check if there is a dependency file created for the open COBOL
             * file, if not send empty array, if yes call prioritizer.checkCopybooksPresentLocal
             * If the dependency file is not present at the initialize moment, the server after analyzing
             * the open file has to ask for settings in order to resolve the local copybooks
             */

             console.log(this.prioritizer.getLocalCpyURI());
            //TODO: get the dependency file content and apply the prioritazion..


            //TODO: Should return an object in the form {cpy_name, uri}
            return (await this.copybooksPathGenerator.listUris()).map(uri => uri.toString());
            // TODO else return next(params, token);
        };
        const configurationMiddleware: ConfigurationWorkspaceMiddleware = {
            configuration: signatureFunc,
        };

        return {
            documentSelector: [LANGUAGE_ID],
            middleware: { workspace: configurationMiddleware }
        };
    }

    private checkMFSettings(): boolean {
      return vscode.workspace.getConfiguration(SETTINGS_SECTION).has(PATHS_ZOWE);
    }

    private createServerOptions(jarPath: string) {
        const port = this.getLspPort();
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
}
