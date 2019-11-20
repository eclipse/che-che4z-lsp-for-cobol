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

package com.broadcom.impl;

import com.broadcom.lsp.cdi.LangServerCtx;
import com.broadcom.lsp.domain.cobol.databus.api.CopybookRepository;
import com.broadcom.lsp.domain.cobol.databus.impl.DefaultDataBusBroker;
import com.broadcom.lsp.domain.cobol.model.CopybookStorable;
import com.broadcom.lsp.domain.cobol.model.DataEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.*;

/** This tests verifies that serialization to a cache storable object works correctly. */
@Slf4j
public class DataBusStoreHappyTest extends AbsDataBusImplTest {
  // cache dummy static content
  private static final String CPY_FIXED_NAME = "COPY-";
  private static final String CPY_FIXED_CONTENT = "FASDFASDFSF";
  private static final String CPY_FIXED_URI = "/var/tmp/worspace1";

  private DefaultDataBusBroker databus;

  @Before
  public void setUp() {
    // create a dummy Multimap

    databus =
        LangServerCtx.getGuiceCtx(new DatabusTestModule())
            .getInjector()
            .getInstance(DefaultDataBusBroker.class);

    fulfillDatabusCacheContent(databus.getCacheMaxSize());

    // add one more element
    databus.storeData(
        CopybookStorable.builder()
            .name("COPY-COPYBOOK")
            .content(CPY_FIXED_CONTENT)
            .uri(CPY_FIXED_URI)
            .build());
  }

  @After
  public void tearDown() {
    databus = null;
    LangServerCtx.shutdown();
  }

  @Override
  public void observerCallback(DataEvent adaptedDataEvent) {
    LOG.debug(String.format("Received : %s", adaptedDataEvent.getEventType().getId()));
  }

  @Test
  @SneakyThrows
  public void cacheData() {
    String newCopybookName = "COPY-" + (databus.getCacheMaxSize() + 1);
    assertFalse(
        databus.isStored(CopybookRepository.calculateUUID(new StringBuilder(newCopybookName))));
    LOG.debug(String.format("Cache content : %s", databus.printCache()));
    Optional<CopybookStorable> leastRecentlyUsed = databus.lastRecentlyUsed();
    LOG.debug(
        String.format(
            "Least Recently Used item : %s  ID : %d",
            leastRecentlyUsed.get().getName(), leastRecentlyUsed.get().getId()));
    // Cache is Full
    LOG.debug(
        String.format(
            "Cache STATUS --> MaxCacheSize: %d  ActualCacheSize: %d",
            databus.getCacheMaxSize(), databus.cacheSize()));
    assertEquals(databus.getCacheMaxSize(), databus.cacheSize());

    LOG.debug(String.format("Storing new item %s ", newCopybookName));
    databus.storeData(
        CopybookStorable.builder()
            .name(newCopybookName)
            .content(CPY_FIXED_CONTENT)
            .uri(CPY_FIXED_URI)
            .build());
    assertTrue(
        databus.isStored(CopybookRepository.calculateUUID(new StringBuilder(newCopybookName))));
    // Swapped
    assertEquals(databus.getCacheMaxSize(), databus.cacheSize());
    LOG.debug(String.format("Cache content : %s", databus.printCache()));
  }

  @Test
  public void invalidateCacheTest() {
    LOG.info("Current cache sizing is: " + databus.getCopybookRepo().size());
    LOG.info("Apply cache invalidation...");
    databus.getCopybookRepo().invalidateCache();
    LOG.info("New cache sizing is: " + databus.getCopybookRepo().size());
    assertEquals(0, databus.getCopybookRepo().size());
  }

  @Test
  public void geteElementFromCache() {
    String element = "COPY-1";
    assertTrue(databus.isStored(CopybookRepository.calculateUUID(new StringBuilder(element))));
  }

  private void fulfillDatabusCacheContent(int cacheMaxSize) {
    for (int i = 0; i < cacheMaxSize; i++) {
      databus.storeData(
          CopybookStorable.builder()
              .name(CPY_FIXED_NAME + i)
              .content("FASDFASDFSF")
              .uri("/var/tmp/worspace1")
              .build());
    }
  }
}
