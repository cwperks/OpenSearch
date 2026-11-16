/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.script.mustache;

import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class BoundedWriterTests extends OpenSearchTestCase {
    public void testAllWritePathsRespectLimit() throws IOException {
        var writer = new BoundedWriter(7);
        writer.write('a');
        writer.write(new char[] { 'x', 'b', 'c' }, 1, 2);
        writer.write("xde", 1, 2);
        writer.append("fg");
        assertEquals("abcdefg", writer.toString());
        expectThrows(IOException.class, () -> writer.write('h'));
        expectThrows(IOException.class, () -> writer.write(new char[] { 'h' }, 0, 1));
        expectThrows(IOException.class, () -> writer.write("h"));
        expectThrows(IOException.class, () -> writer.append("h"));
        assertEquals("abcdefg", writer.toString());
    }

    public void testRejectedWriteDoesNotAppendPartialOutput() throws IOException {
        var writer = new BoundedWriter(2);
        writer.write("a");
        expectThrows(IOException.class, () -> writer.write("bc"));
        writer.write("b");
        assertEquals("ab", writer.toString());
    }
}
