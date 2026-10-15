/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.admin.listener;

import org.apache.shenyu.common.constant.DefaultPathConstants;
import org.apache.shenyu.common.dto.DiscoverySyncData;
import org.apache.shenyu.common.enums.DataEventTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbstractPathDataChangedListenerTest {

    @Test
    public void testOnDiscoveryUpstreamChangedIgnoresDeleteWithEmptyPluginName() {
        DiscoverySyncData discoverySyncData = new DiscoverySyncData();
        discoverySyncData.setNamespaceId("default");
        discoverySyncData.setSelectorId("selector-id");
        discoverySyncData.setPluginName("");
        TestPathDataChangedListener listener = new TestPathDataChangedListener();

        listener.onDiscoveryUpstreamChanged(Collections.singletonList(discoverySyncData), DataEventTypeEnum.DELETE);

        assertNull(listener.deletedPath);
    }

    @Test
    public void testOnDiscoveryUpstreamChangedIgnoresUpdateWithEmptyPluginName() {
        DiscoverySyncData discoverySyncData = new DiscoverySyncData();
        discoverySyncData.setNamespaceId("default");
        discoverySyncData.setSelectorId("selector-id");
        discoverySyncData.setPluginName("");
        TestPathDataChangedListener listener = new TestPathDataChangedListener();

        listener.onDiscoveryUpstreamChanged(Collections.singletonList(discoverySyncData), DataEventTypeEnum.UPDATE);

        assertNull(listener.updatedPath);
    }

    @Test
    public void testOnDiscoveryUpstreamChangedIgnoresDeleteWithMissingSelectorId() {
        DiscoverySyncData discoverySyncData = new DiscoverySyncData();
        discoverySyncData.setNamespaceId("default");
        discoverySyncData.setPluginName("divide");
        TestPathDataChangedListener listener = new TestPathDataChangedListener();

        listener.onDiscoveryUpstreamChanged(Collections.singletonList(discoverySyncData), DataEventTypeEnum.DELETE);

        assertNull(listener.deletedPath);
    }

    @Test
    public void testOnDiscoveryUpstreamChangedIgnoresUpdateWithBlankSelectorId() {
        DiscoverySyncData discoverySyncData = new DiscoverySyncData();
        discoverySyncData.setNamespaceId("default");
        discoverySyncData.setSelectorId(" ");
        discoverySyncData.setPluginName("divide");
        TestPathDataChangedListener listener = new TestPathDataChangedListener();

        listener.onDiscoveryUpstreamChanged(Collections.singletonList(discoverySyncData), DataEventTypeEnum.UPDATE);

        assertNull(listener.updatedPath);
    }

    @Test
    public void testOnDiscoveryUpstreamChangedDeletesValidPath() {
        DiscoverySyncData discoverySyncData = new DiscoverySyncData();
        discoverySyncData.setNamespaceId("default");
        discoverySyncData.setSelectorId("selector-id");
        discoverySyncData.setPluginName("divide");
        TestPathDataChangedListener listener = new TestPathDataChangedListener();

        listener.onDiscoveryUpstreamChanged(Collections.singletonList(discoverySyncData), DataEventTypeEnum.DELETE);

        assertEquals("/default/shenyu/discoveryUpstream/divide/selector-id", listener.deletedPath);
    }

    @Test
    public void testNamespaceEmptySelectorRefreshRemovesStaleData() {
        TestPathDataChangedListener listener = new TestPathDataChangedListener();
        String namespaceAPath = DefaultPathConstants.buildSelectorRealPath("namespace-a", "divide", "stale");
        String namespaceBPath = DefaultPathConstants.buildSelectorRealPath("namespace-b", "divide", "stale");
        listener.seedPath(namespaceAPath);
        listener.seedPath(namespaceBPath);

        listener.onSelectorChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertFalse(listener.hasPath(namespaceAPath)),
                () -> assertTrue(listener.hasPath(namespaceBPath)));
    }

    @Test
    public void testNamespaceEmptyRuleRefreshRemovesStaleData() {
        TestPathDataChangedListener listener = new TestPathDataChangedListener();
        String namespaceAPath = DefaultPathConstants.buildRulePath("namespace-a", "divide", "selector-id", "stale");
        String namespaceBPath = DefaultPathConstants.buildRulePath("namespace-b", "divide", "selector-id", "stale");
        listener.seedPath(namespaceAPath);
        listener.seedPath(namespaceBPath);

        listener.onRuleChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertFalse(listener.hasPath(namespaceAPath)),
                () -> assertTrue(listener.hasPath(namespaceBPath)));
    }

    @Test
    public void testNamespaceEmptyAppAuthRefreshRemovesStaleData() {
        TestPathDataChangedListener listener = new TestPathDataChangedListener();
        String namespaceAPath = DefaultPathConstants.buildAppAuthPath("namespace-a", "stale-app-key");
        String namespaceBPath = DefaultPathConstants.buildAppAuthPath("namespace-b", "stale-app-key");
        listener.seedPath(namespaceAPath);
        listener.seedPath(namespaceBPath);

        listener.onAppAuthChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertFalse(listener.hasPath(namespaceAPath)),
                () -> assertTrue(listener.hasPath(namespaceBPath)));
    }

    @Test
    public void testNamespaceEmptyPluginRefreshRemovesStaleData() {
        TestPathDataChangedListener listener = new TestPathDataChangedListener();
        String namespaceAPath = DefaultPathConstants.buildPluginPath("namespace-a", "divide");
        String namespaceBPath = DefaultPathConstants.buildPluginPath("namespace-b", "divide");
        listener.seedPath(namespaceAPath);
        listener.seedPath(namespaceBPath);

        listener.onPluginChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertFalse(listener.hasPath(namespaceAPath)),
                () -> assertTrue(listener.hasPath(namespaceBPath)));
    }

    private static final class TestPathDataChangedListener extends AbstractPathDataChangedListener {

        private final Set<String> paths = new HashSet<>();

        private String deletedPath;

        private String updatedPath;

        @Override
        public void createOrUpdate(final String pluginPath, final Object data) {
            updatedPath = pluginPath;
        }

        @Override
        public void deleteNode(final String pluginPath) {
            deletedPath = pluginPath;
            paths.remove(pluginPath);
        }

        @Override
        public void deletePathRecursive(final String path) {
            paths.removeIf(currentPath -> currentPath.equals(path) || currentPath.startsWith(path + "/"));
        }

        private void seedPath(final String path) {
            paths.add(path);
        }

        private boolean hasPath(final String path) {
            return paths.contains(path);
        }
    }
}
