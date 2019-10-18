/*
 * Copyright (c) 2019 Broadcom.
 *
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Broadcom, Inc. - initial API and implementation
 *
 */

package com.broadcom.lsp.domain.cobol.databus.impl;

import com.broadcom.lsp.domain.cobol.databus.api.DefaultEventSubScriberImpl;
import lombok.extern.slf4j.Slf4j;


/**
 * Created  on 15/10/2019
 */
@Slf4j
public class IStringSubScriberImpl<String> extends DefaultEventSubScriberImpl<String> {
    @Override
    public void onDataHandler(String data) {
        LOG.info("test subscribe");
    }
}
