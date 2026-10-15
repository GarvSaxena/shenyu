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

import org.apache.shenyu.common.constant.DefaultNodeConstants;
import org.apache.shenyu.common.dto.PluginData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.common.enums.DataEventTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.apache.shenyu.common.constant.Constants.SYS_DEFAULT_NAMESPACE_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AbstractNodeDataChangedListenerTest {

    private static final String NAMESPACE_ID = "namespace";

    private static final String FIRST_PLUGIN = "firstPlugin";

    private static final String SECOND_PLUGIN = "secondPlugin";

    private static final String RETAINED_ID = "retained";

    private static final String ADDED_ID = "added";

    private static final String STALE_ID = "stale";

    @Test
    public void testOnSelectorChangedRefreshReplacesPluginSelectorIds() {
        TestNodeDataChangedListener listener = new TestNodeDataChangedListener();
        listener.putConfig(selectorListKey(FIRST_PLUGIN), Arrays.asList(RETAINED_ID, STALE_ID));
        listener.putConfig(selectorListKey(SECOND_PLUGIN), Arrays.asList(RETAINED_ID, STALE_ID));
        listener.putConfig(selectorDataKey(FIRST_PLUGIN, STALE_ID), new SelectorData());
        listener.putConfig(selectorDataKey(SECOND_PLUGIN, STALE_ID), new SelectorData());

        listener.onSelectorChanged(Arrays.asList(
                selectorData(FIRST_PLUGIN, RETAINED_ID),
                selectorData(SECOND_PLUGIN, RETAINED_ID),
                selectorData(FIRST_PLUGIN, ADDED_ID)), DataEventTypeEnum.REFRESH);

        assertEquals(Arrays.asList(RETAINED_ID, ADDED_ID), listener.config(selectorListKey(FIRST_PLUGIN)));
        assertEquals(Arrays.asList(RETAINED_ID), listener.config(selectorListKey(SECOND_PLUGIN)));
        assertNull(listener.config(selectorDataKey(FIRST_PLUGIN, STALE_ID)));
        assertNull(listener.config(selectorDataKey(SECOND_PLUGIN, STALE_ID)));
        assertTrue(listener.wasDeleted(selectorDataKey(FIRST_PLUGIN, STALE_ID)));
        assertTrue(listener.wasDeleted(selectorDataKey(SECOND_PLUGIN, STALE_ID)));
    }

    @Test
    public void testNamespaceEmptySelectorRefreshRemovesStaleData() {
        TestNodeDataChangedListener listener = new TestNodeDataChangedListener();
        String namespaceAListKey = "namespace-a.selector." + FIRST_PLUGIN + DefaultNodeConstants.POINT_LIST;
        String namespaceADataKey = "namespace-a.selector." + FIRST_PLUGIN + DefaultNodeConstants.JOIN_POINT + STALE_ID;
        String namespaceBListKey = "namespace-b.selector." + FIRST_PLUGIN + DefaultNodeConstants.POINT_LIST;
        String namespaceBDataKey = "namespace-b.selector." + FIRST_PLUGIN + DefaultNodeConstants.JOIN_POINT + STALE_ID;
        listener.putConfig("namespace-a.plugin.list", Collections.singletonList(FIRST_PLUGIN));
        listener.putConfig("namespace-b.plugin.list", Collections.singletonList(FIRST_PLUGIN));
        listener.putConfig(namespaceAListKey, Collections.singletonList(STALE_ID));
        listener.putConfig(namespaceADataKey, selectorData("namespace-a", FIRST_PLUGIN, STALE_ID));
        listener.putConfig(namespaceBListKey, Collections.singletonList(STALE_ID));
        listener.putConfig(namespaceBDataKey, selectorData("namespace-b", FIRST_PLUGIN, STALE_ID));

        listener.onSelectorChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertNull(listener.config(namespaceAListKey)),
                () -> assertNull(listener.config(namespaceADataKey)),
                () -> assertEquals(Collections.singletonList(STALE_ID), listener.config(namespaceBListKey)),
                () -> assertEquals(selectorData("namespace-b", FIRST_PLUGIN, STALE_ID), listener.config(namespaceBDataKey)));
    }

    @Test
    public void testNamespaceEmptyRuleRefreshRemovesStaleData() {
        TestNodeDataChangedListener listener = new TestNodeDataChangedListener();
        String namespaceAListKey = "namespace-a.rule." + FIRST_PLUGIN + ".selector-id" + DefaultNodeConstants.POINT_LIST;
        String namespaceADataKey = "namespace-a.rule." + FIRST_PLUGIN + ".selector-id.stale";
        String namespaceBListKey = "namespace-b.rule." + FIRST_PLUGIN + ".selector-id" + DefaultNodeConstants.POINT_LIST;
        String namespaceBDataKey = "namespace-b.rule." + FIRST_PLUGIN + ".selector-id.stale";
        listener.putConfig("namespace-a.plugin.list", Collections.singletonList(FIRST_PLUGIN));
        listener.putConfig("namespace-b.plugin.list", Collections.singletonList(FIRST_PLUGIN));
        listener.putConfig("namespace-a.selector." + FIRST_PLUGIN + DefaultNodeConstants.POINT_LIST,
                Collections.singletonList("selector-id"));
        listener.putConfig("namespace-b.selector." + FIRST_PLUGIN + DefaultNodeConstants.POINT_LIST,
                Collections.singletonList("selector-id"));
        listener.putConfig(namespaceAListKey, Collections.singletonList(STALE_ID));
        listener.putConfig(namespaceADataKey, "stale-rule");
        listener.putConfig(namespaceBListKey, Collections.singletonList(STALE_ID));
        listener.putConfig(namespaceBDataKey, "stale-rule");

        listener.onRuleChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertNull(listener.config(namespaceAListKey)),
                () -> assertNull(listener.config(namespaceADataKey)),
                () -> assertEquals(Collections.singletonList(STALE_ID), listener.config(namespaceBListKey)),
                () -> assertEquals("stale-rule", listener.config(namespaceBDataKey)));
    }

    @Test
    public void testNamespaceEmptyAppAuthRefreshRemovesStaleData() {
        TestNodeDataChangedListener listener = new TestNodeDataChangedListener();
        String namespaceAListKey = "namespace-a.auth.list";
        String namespaceADataKey = "namespace-a.auth.stale-app-key";
        String namespaceBListKey = "namespace-b.auth.list";
        String namespaceBDataKey = "namespace-b.auth.stale-app-key";
        listener.putConfig(namespaceAListKey, Collections.singletonList("stale-app-key"));
        listener.putConfig(namespaceADataKey, "stale-auth");
        listener.putConfig(namespaceBListKey, Collections.singletonList("stale-app-key"));
        listener.putConfig(namespaceBDataKey, "stale-auth");

        listener.onAppAuthChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertNull(listener.config(namespaceADataKey)),
                () -> assertEquals(Collections.emptyList(), listener.config(namespaceAListKey)),
                () -> assertEquals(Collections.singletonList("stale-app-key"), listener.config(namespaceBListKey)),
                () -> assertEquals("stale-auth", listener.config(namespaceBDataKey)));
    }

    @Test
    public void testNamespaceEmptyPluginRefreshRemovesStaleData() {
        TestNodeDataChangedListener listener = new TestNodeDataChangedListener();
        String namespaceAListKey = "namespace-a.plugin.list";
        String namespaceADataKey = "namespace-a.plugin.stale-plugin";
        String namespaceBListKey = "namespace-b.plugin.list";
        String namespaceBDataKey = "namespace-b.plugin.stale-plugin";
        listener.putConfig(namespaceAListKey, Collections.singletonList("stale-plugin"));
        listener.putConfig(namespaceADataKey, "stale-plugin");
        listener.putConfig(namespaceBListKey, Collections.singletonList("stale-plugin"));
        listener.putConfig(namespaceBDataKey, "stale-plugin");

        listener.onPluginChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a");

        assertAll(
                () -> assertNull(listener.config(namespaceADataKey)),
                () -> assertEquals(Collections.emptyList(), listener.config(namespaceAListKey)),
                () -> assertEquals(Collections.singletonList("stale-plugin"), listener.config(namespaceBListKey)),
                () -> assertEquals("stale-plugin", listener.config(namespaceBDataKey)));
    }

    @Test
    public void testNamespaceEmptyRefreshPropagatesStorageFailures() {
        TestNodeDataChangedListener commonListener = new TestNodeDataChangedListener();
        String authDataKey = "namespace-a.auth.stale-app-key";
        commonListener.putConfig("namespace-a.auth.list", Collections.singletonList("stale-app-key"));
        commonListener.putConfig(authDataKey, "stale-auth");
        commonListener.failOnDelete(authDataKey);
        assertThrows(IllegalStateException.class,
                () -> commonListener.onAppAuthChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a"));

        TestNodeDataChangedListener selectorListener = new TestNodeDataChangedListener();
        String selectorDataKey = "namespace-a.selector." + FIRST_PLUGIN + DefaultNodeConstants.JOIN_POINT + STALE_ID;
        selectorListener.putConfig("namespace-a.plugin.list", Collections.singletonList(FIRST_PLUGIN));
        selectorListener.putConfig(selectorListKeyForNamespace("namespace-a", FIRST_PLUGIN), Collections.singletonList(STALE_ID));
        selectorListener.putConfig(selectorDataKey, "stale-selector");
        selectorListener.failOnDelete(selectorDataKey);
        assertThrows(IllegalStateException.class,
                () -> selectorListener.onSelectorChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a"));

        TestNodeDataChangedListener ruleListener = new TestNodeDataChangedListener();
        String ruleListKey = "namespace-a.rule." + FIRST_PLUGIN + ".selector-id" + DefaultNodeConstants.POINT_LIST;
        String ruleDataKey = "namespace-a.rule." + FIRST_PLUGIN + ".selector-id." + STALE_ID;
        ruleListener.putConfig("namespace-a.plugin.list", Collections.singletonList(FIRST_PLUGIN));
        ruleListener.putConfig(selectorListKeyForNamespace("namespace-a", FIRST_PLUGIN), Collections.singletonList("selector-id"));
        ruleListener.putConfig(ruleListKey, Collections.singletonList(STALE_ID));
        ruleListener.putConfig(ruleDataKey, "stale-rule");
        ruleListener.failOnDelete(ruleDataKey);
        assertThrows(IllegalStateException.class,
                () -> ruleListener.onRuleChanged(Collections.emptyList(), DataEventTypeEnum.REFRESH, "namespace-a"));
    }

    @Test
    public void testOnPluginChangedRefreshWithEqualCardinalityRemovesStaleEntries() {
        TestNodeDataChangedListener listener = new TestNodeDataChangedListener();
        final String configKeyPrefix = NAMESPACE_ID + DefaultNodeConstants.JOIN_POINT + "plugin" + DefaultNodeConstants.JOIN_POINT;
        listener.putConfig(configKeyPrefix + DefaultNodeConstants.LIST_STR, Arrays.asList("A", "B", "C", "D"));
        listener.putConfig(configKeyPrefix + "A", pluginData("A"));
        listener.putConfig(configKeyPrefix + "B", pluginData("B"));

        listener.onPluginChanged(Arrays.asList(
                pluginData("C"), pluginData("D"), pluginData("E"), pluginData("F")), DataEventTypeEnum.REFRESH);

        assertNull(listener.config(configKeyPrefix + "A"));
        assertNull(listener.config(configKeyPrefix + "B"));
        assertTrue(listener.wasDeleted(configKeyPrefix + "A"));
        assertTrue(listener.wasDeleted(configKeyPrefix + "B"));
    }

    @Test
    public void testOnSelectorChangedWithNullNamespaceUsesDefaultNamespace() {
        TestNodeDataChangedListener listener = new TestNodeDataChangedListener();
        SelectorData selectorData = SelectorData.builder()
                .pluginName(FIRST_PLUGIN)
                .id(ADDED_ID)
                .build();

        listener.onSelectorChanged(Collections.singletonList(selectorData), DataEventTypeEnum.UPDATE);

        final String configKeyPrefix = SYS_DEFAULT_NAMESPACE_ID + DefaultNodeConstants.JOIN_POINT + "selector" + DefaultNodeConstants.JOIN_POINT;
        assertEquals(Collections.singletonList(ADDED_ID),
                listener.config(configKeyPrefix + FIRST_PLUGIN + DefaultNodeConstants.POINT_LIST));
        assertEquals(selectorData,
                listener.config(configKeyPrefix + FIRST_PLUGIN + DefaultNodeConstants.JOIN_POINT + ADDED_ID));
    }

    private static PluginData pluginData(final String name) {
        return PluginData.builder()
                .namespaceId(NAMESPACE_ID)
                .name(name)
                .build();
    }

    private static SelectorData selectorData(final String pluginName, final String selectorId) {
        return selectorData(NAMESPACE_ID, pluginName, selectorId);
    }

    private static SelectorData selectorData(final String namespaceId, final String pluginName, final String selectorId) {
        return SelectorData.builder()
                .namespaceId(namespaceId)
                .pluginName(pluginName)
                .id(selectorId)
                .build();
    }

    private static String selectorListKey(final String pluginName) {
        return NAMESPACE_ID + ".selector." + pluginName + DefaultNodeConstants.POINT_LIST;
    }

    private static String selectorDataKey(final String pluginName, final String selectorId) {
        return NAMESPACE_ID + ".selector." + pluginName + DefaultNodeConstants.JOIN_POINT + selectorId;
    }

    private static String selectorListKeyForNamespace(final String namespaceId, final String pluginName) {
        return namespaceId + ".selector." + pluginName + DefaultNodeConstants.POINT_LIST;
    }

    private static final class TestNodeDataChangedListener extends AbstractNodeDataChangedListener {

        private final Map<String, Object> configs = new HashMap<>();

        private final Set<String> deletedDataIds = new HashSet<>();

        private String failedDataId;

        private TestNodeDataChangedListener() {
            super(new ChangeData("plugin", "selector", "rule", "auth", "meta", "proxy.selector", "discovery"));
        }

        @Override
        public void doPublishConfig(final String dataId, final Object data) {
            configs.put(dataId, data);
        }

        @Override
        protected void doDelConfig(final String dataId) {
            if (dataId.equals(failedDataId)) {
                throw new IllegalStateException("test storage failure");
            }
            configs.remove(dataId);
            deletedDataIds.add(dataId);
        }

        @Override
        public String getConfig(final String dataId) {
            return Optional.ofNullable(configs.get(dataId))
                    .map(GsonUtils.getInstance()::toJson)
                    .orElse(null);
        }

        private void putConfig(final String dataId, final Object data) {
            configs.put(dataId, data);
        }

        private void failOnDelete(final String dataId) {
            failedDataId = dataId;
        }

        private Object config(final String dataId) {
            return configs.get(dataId);
        }

        private boolean wasDeleted(final String dataId) {
            return deletedDataIds.contains(dataId);
        }
    }
}
