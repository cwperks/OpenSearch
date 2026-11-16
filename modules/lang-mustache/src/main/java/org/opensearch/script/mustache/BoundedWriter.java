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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;

/** Bounds rendered output before appending it to the backing buffer. */
final class BoundedWriter extends Writer {
    private final StringWriter delegate = new StringWriter();
    private final int maxChars;

    BoundedWriter(int maxChars) {
        this.maxChars = maxChars;
    }

    private void checkLength(int length) throws IOException {
        if (length > maxChars - delegate.getBuffer().length()) {
            throw new IOException("Mustache output exceeds [script.mustache.max_output_chars] limit of [" + maxChars + "] characters");
        }
    }

    @Override
    public void write(char[] chars, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, chars.length);
        checkLength(length);
        delegate.write(chars, offset, length);
    }

    @Override
    public void write(String value, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, value.length());
        checkLength(length);
        delegate.write(value, offset, length);
    }

    @Override
    public void write(int character) throws IOException {
        checkLength(1);
        delegate.write(character);
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}

    @Override
    public String toString() {
        return delegate.toString();
    }
}
