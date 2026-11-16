/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.script.mustache;

import org.opensearch.common.settings.Settings;
import org.opensearch.plugins.Plugin;
import org.opensearch.script.GeneralScriptException;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptService;
import org.opensearch.script.ScriptType;
import org.opensearch.script.TemplateScript;
import org.opensearch.test.OpenSearchSingleNodeTestCase;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;

public class MustacheOutputLimitIT extends OpenSearchSingleNodeTestCase {
    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return List.of(MustacheModulePlugin.class);
    }

    public void testDynamicLimitAppliesToCachedTemplates() {
        var service = getInstanceFromNode(ScriptService.class);
        var script = new Script(ScriptType.INLINE, "mustache", "{{{value}}}", Map.of("value", "1234"));
        var factory = service.compile(script, TemplateScript.CONTEXT);
        var template = factory.newInstance(script.getParams());
        String key = MustacheScriptEngine.MAX_OUTPUT_CHARS.getKey();
        try {
            assertAcked(client().admin().cluster().prepareUpdateSettings().setPersistentSettings(Settings.builder().put(key, 3)).get());
            expectThrows(GeneralScriptException.class, template::execute);
            assertSame(factory, service.compile(script, TemplateScript.CONTEXT));
            assertAcked(client().admin().cluster().prepareUpdateSettings().setPersistentSettings(Settings.builder().put(key, 4)).get());
            assertEquals("1234", template.execute());
            expectThrows(
                Exception.class,
                () -> client().admin().cluster().prepareUpdateSettings().setPersistentSettings(Settings.builder().put(key, 0)).get()
            );
        } finally {
            assertAcked(client().admin().cluster().prepareUpdateSettings().setPersistentSettings(Settings.builder().putNull(key)).get());
        }
        assertEquals("x".repeat(100_000), factory.newInstance(Map.of("value", "x".repeat(100_000))).execute());
        expectThrows(GeneralScriptException.class, () -> factory.newInstance(Map.of("value", "x".repeat(100_001))).execute());
    }
}
