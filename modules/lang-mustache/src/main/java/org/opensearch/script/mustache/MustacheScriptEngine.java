/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.script.mustache;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.opensearch.SpecialPermission;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.script.GeneralScriptException;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptEngine;
import org.opensearch.script.ScriptException;
import org.opensearch.script.TemplateScript;
import org.opensearch.secure_sm.AccessController;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Main entry point handling template registration, compilation and
 * execution.
 * <p>
 * Template handling is based on Mustache. Template handling is a two step
 * process: First compile the string representing the template, the resulting
 * {@link Mustache} object can then be re-used for subsequent executions.
 */
public final class MustacheScriptEngine implements ScriptEngine {
    private static final Logger logger = LogManager.getLogger(MustacheScriptEngine.class);

    public static final String NAME = "mustache";

    /** Maximum rendered template length, measured in UTF-16 code units. */
    public static final Setting<Integer> MAX_OUTPUT_CHARS = Setting.intSetting(
        "script.mustache.max_output_chars",
        100_000,
        1,
        Setting.Property.NodeScope,
        Setting.Property.Dynamic
    )
        .withDescription(
            "Maximum rendered Mustache template length in UTF-16 code units. Applies to each new execution, including cached templates."
        );

    private volatile int maxOutputChars;

    /** Creates an engine with default settings. */
    public MustacheScriptEngine() {
        this(Settings.EMPTY);
    }

    /** Creates an engine with the configured output limit. */
    public MustacheScriptEngine(Settings settings) {
        maxOutputChars = MAX_OUTPUT_CHARS.get(settings);
    }

    void setMaxOutputChars(int maxOutputChars) {
        this.maxOutputChars = maxOutputChars;
    }

    /**
     * Compile a template string to (in this case) a Mustache object than can
     * later be re-used for execution to fill in missing parameter values.
     *
     * @param templateSource a string representing the template to compile.
     * @return a compiled template object for later execution.
     * */
    @Override
    public <T> T compile(String templateName, String templateSource, ScriptContext<T> context, Map<String, String> options) {
        if (context.instanceClazz.equals(TemplateScript.class) == false) {
            throw new IllegalArgumentException("mustache engine does not know how to handle context [" + context.name + "]");
        }
        final MustacheFactory factory = createMustacheFactory(options);
        Reader reader = new StringReader(templateSource);
        try {
            Mustache template = factory.compile(reader, "query-template");
            TemplateScript.Factory compiled = params -> new MustacheExecutableScript(template, params);
            return context.factoryClazz.cast(compiled);
        } catch (MustacheException ex) {
            throw new ScriptException(ex.getMessage(), ex, Collections.emptyList(), templateSource, NAME);
        }

    }

    @Override
    public Set<ScriptContext<?>> getSupportedContexts() {
        return Collections.singleton(TemplateScript.CONTEXT);
    }

    private CustomMustacheFactory createMustacheFactory(Map<String, String> options) {
        if (options == null || options.isEmpty() || options.containsKey(Script.CONTENT_TYPE_OPTION) == false) {
            return new CustomMustacheFactory();
        }
        return new CustomMustacheFactory(options.get(Script.CONTENT_TYPE_OPTION));
    }

    @Override
    public String getType() {
        return NAME;
    }

    /**
     * Used at query execution time by script service in order to execute a query template.
     * */
    private class MustacheExecutableScript extends TemplateScript {
        /** Factory template. */
        private Mustache template;

        private Map<String, Object> params;

        /**
         * @param template the compiled template object wrapper
         **/
        MustacheExecutableScript(Mustache template, Map<String, Object> params) {
            super(params);
            this.template = template;
            this.params = params;
        }

        @Override
        public String execute() {
            final BoundedWriter writer = new BoundedWriter(maxOutputChars);
            try {
                // crazy reflection here
                SpecialPermission.check();
                AccessController.doPrivileged(() -> template.execute(writer, params));
            } catch (Exception e) {
                logger.error((Supplier<?>) () -> new ParameterizedMessage("Error running {}", template), e);
                throw new GeneralScriptException("Error running " + template, e);
            }
            return writer.toString();
        }
    }
}
