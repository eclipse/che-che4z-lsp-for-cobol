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
import * as vscode from "vscode";
import {
    CONN_REFUSED_ERROR_MSG,
    DEPENDENCIES_FOLDER,
    DSN_NOT_FOUND_ERROR_MSG,
    DSN_PLACEHOLDER,
    INVALID_CREDENTIALS_ERROR_MSG,
    NO_PASSWORD_ERROR_MSG,
    PROCESS_DOWNLOAD_ERROR_MSG,
    PROFILE_NAME_PLACEHOLDER,
} from "../constants";
import {CopybookFix} from "./CopybookFix";
import {checkWorkspace, CopybooksPathGenerator, createCopybookPath, createDatasetPath} from "./CopybooksPathGenerator";
import {DependenciesDesc, loadDepFile} from "./DependencyService";
import {CopybookProfile, DownloadQueue} from "./DownloadQueue";
import {ProfileService} from "./ProfileService";
import {ZoweApi} from "./ZoweApi";
import {Type, ZoweError} from "./ZoweError";

export class CopybooksDownloader implements vscode.Disposable {
    private queue: DownloadQueue = new DownloadQueue();

    public constructor(
        private resolver: CopybookFix,
        private zoweApi: ZoweApi,
        private profileService: ProfileService,
        private pathGenerator: CopybooksPathGenerator) {
    }

    public async redownloadDependencies(message: string = "Redownload dependencies requested.") {
        //TODO: related to zowe refactor - Should comunicate the server to recalculate the copybooks?
        (await vscode.workspace.findFiles(DEPENDENCIES_FOLDER + "/**/*.dep")).forEach(dep => {
            this.downloadDependencies(dep, message);
        });
    }

    /**
     * @param copybooks array of copybooks names to download
     * @param programName analyzed COBOL file
     */
    async downloadCopybooks(copybooks: string[], programName: string): Promise<void> {
        const profile: string = await this.profileService.getProfile(programName);
        if (!profile) {
            return;
        }
        copybooks.forEach(copybook => this.queue.push(copybook, profile));
    }

    public async downloadDependency(cobolFileName: string, copybookName: string): Promise<void> {
        if (!checkWorkspace()) {
            return;
        }
        const profile: string = await this.profileService.getProfile(cobolFileName);
        if (!profile) {
            return;
        }
        //async needed?
        this.resolver.downloadMissingCopybooks([copybookName], profile);

    }

    public async downloadDependencies(depFileUri: vscode.Uri, message: string = ""): Promise<void> {
        if (!checkWorkspace()) {
            return;
        }
        const depDesc: DependenciesDesc = loadDepFile(depFileUri);

        // TODO: REMOVE PROFILE OR ADD IT AFTER LOCAL CHECK
        const profile: string = await this.profileService.getProfile(depDesc.programName);
        if (!profile) {
            return;
        }

        const missingCopybooks: string[] = await this.listMissingCopybooks(depDesc.copybooks, profile);

        if (!message.length) {
            missingCopybooks.forEach(copybook => this.queue.push(copybook, profile));
        } else if (missingCopybooks.length > 0) {
            this.resolver.fixMissingDownloads(missingCopybooks, profile, {
                hasPaths: (await this.pathGenerator.listDatasets()).length > 0,
                hasProfiles: Object.keys(await this.profileService.listProfiles()).length > 1,
            }, message);
        }
    }

    public async start() {
        this.resolver.setQueue(this.queue);
        let done = false;
        const errors = new Set();
        while (!done) {
            const element: CopybookProfile | undefined = await this.queue.pop();
            if (!element) {
                done = true;
                continue;
            }
            await vscode.window.withProgress(
                {
                    location: vscode.ProgressLocation.Notification,
                    title: "Fetching copybooks",
                },
                async (progress: vscode.Progress<{ message?: string; increment?: number }>) => {
                    const toDownload: CopybookProfile[] = [element];
                    while (this.queue.length > 0) {
                        toDownload.push(await this.queue.pop());
                    }
                    toDownload.map(cp => cp.copybook).forEach(cb => errors.add(cb));
                    for (const dataset of await this.pathGenerator.listDatasets()) {
                        progress.report({
                            message: "Looking in " + dataset + ". " + toDownload.length +
                                " copybook(s) left.",
                        });
                        for (const cp of toDownload) {
                            try {
                                const fetchResult = await this.fetchCopybook(dataset, cp);
                                if (fetchResult) {
                                    errors.delete(cp.copybook);
                                }
                            } catch (e) {
                                vscode.window.showErrorMessage(e.toString());
                            }
                        }
                    }
                    if (this.queue.length === 0 && errors.size > 0) {
                        this.resolver.processDownloadError(PROCESS_DOWNLOAD_ERROR_MSG + Array.from(errors));
                        errors.clear();
                    }
                });
        }
    }

    public dispose() {
        this.queue.stop();
    }

    private async handleQueue(
        element: CopybookProfile,
        errors: Set<string>,
        progress: vscode.Progress<{ message?: string; increment?: number }>) {
        const toDownload: CopybookProfile[] = [element];
        while (this.queue.length > 0) {
            toDownload.push(await this.queue.pop());
        }
        toDownload.map(cp => cp.copybook).forEach(cb => errors.add(cb));
        try {
            for (const dataset of await this.pathGenerator.listDatasets()) {
                await this.handleDataset(dataset, toDownload, errors, progress);
            }
        } catch (e) {
            let errorMessage = e.toString();
            if (e instanceof ZoweError) {
                switch (e.type) {
                    case Type.InvalidCredentials:
                        errorMessage = INVALID_CREDENTIALS_ERROR_MSG.replace(PROFILE_NAME_PLACEHOLDER, element.profile);
                        break;
                    case Type.ConnRefused:
                        errorMessage = CONN_REFUSED_ERROR_MSG.replace(PROFILE_NAME_PLACEHOLDER, element.profile);
                        break;
                    case Type.NoPassword:
                        errorMessage = NO_PASSWORD_ERROR_MSG.replace(PROFILE_NAME_PLACEHOLDER, element.profile);
                        break;
                    default:
                        break;
                }
            }
            vscode.window.showErrorMessage(errorMessage);
        }
    }

    private async handleDataset(
        dataset: string,
        toDownload: CopybookProfile[],
        errors: Set<string>,
        progress: vscode.Progress<{ message?: string; increment?: number }>) {
        try {
            progress.report({
                message: "Looking in " + dataset + ". " + toDownload.length +
                    " copybook(s) left.",
            });
            for (const cp of toDownload) {
                await this.handleCopybook(dataset, cp, errors);
            }
        } catch (e) {
            let errorMessage = e.toString();
            if (e instanceof ZoweError) {
                if (e.type === Type.NotFound) {
                    errorMessage = DSN_NOT_FOUND_ERROR_MSG.replace(DSN_PLACEHOLDER, dataset);
                } else {
                    throw e;
                }
            }
            vscode.window.showErrorMessage(errorMessage);
        }
    }

    private async handleCopybook(dataset: string, cp: CopybookProfile, errors: Set<string>) {
        try {
            const fetchResult = await this.fetchCopybook(dataset, cp);
            if (fetchResult) {
                errors.delete(cp.copybook);
            }
        } catch (e) {
            if (e instanceof ZoweError) {
                throw e;
            }
            vscode.window.showErrorMessage(e.toString());
        }
    }

    private async fetchCopybook(dataset: string, copybookProfile: CopybookProfile): Promise<boolean> {
        let members: string[] = [];
        try {
            members = await this.zoweApi.listMembers(dataset, copybookProfile.profile);
        } catch (error) {
            if (error instanceof ZoweError) {
                throw error;
            }
            await this.resolver.processDownloadError("Can't read members of dataset: " + dataset);
            return false;
        }
        if (!members.includes(copybookProfile.copybook)) {
            return false;
        }
        await this.downloadCopybook(dataset, copybookProfile.copybook, copybookProfile.profile);
        return true;
    }

    private async downloadCopybook(dataset: string, copybook: string, profileName: string) {
        const copybookPath = createCopybookPath(profileName, dataset, copybook);
        if (!fs.existsSync(copybookPath)) {
            const content = await this.zoweApi.fetchMember(dataset, copybook, profileName);
            fs.mkdirSync(createDatasetPath(profileName, dataset), {recursive: true});
            fs.writeFileSync(copybookPath, content);
        }
    }

    private async listMissingCopybooks(copybooks: string[], profileName: string): Promise<string[]> {
        const copybooksToDownload: Set<string> = new Set(copybooks);
        (await this.pathGenerator.listDatasets()).forEach(ds => {
            Array.from(copybooksToDownload.values()).forEach(c => {
                if (fs.existsSync(createCopybookPath(profileName, ds, c))) {
                    copybooksToDownload.delete(c);
                }
            });
        });

        return Array.from(copybooksToDownload.values());
    }
}
