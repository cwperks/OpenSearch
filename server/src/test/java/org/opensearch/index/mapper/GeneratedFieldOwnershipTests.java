/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.index.mapper;

import org.opensearch.common.Explicit;
import org.opensearch.common.lucene.Lucene;
import org.opensearch.test.OpenSearchTestCase;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeneratedFieldOwnershipTests extends OpenSearchTestCase {
    public void testGeneratedCompanionObjects() {
        for (String companion : List.of("title_semantic_info", "custom_info", "details.custom_info")) {
            String owner = "details.title";
            String text = companion + ".chunks.text";
            MappingLookup lookup = lookup(
                List.of(generatingField(owner, companion), new MockFieldMapper(text), new MockFieldMapper("public_field")),
                List.of(object(companion, false), object(companion + ".chunks", true)),
                List.of(new FieldAliasMapper("text_alias", "text_alias", text))
            );
            assertEquals(owner, lookup.generatedFieldOwner(companion));
            assertEquals(owner, lookup.generatedFieldOwner(companion + ".chunks"));
            assertEquals(owner, lookup.generatedFieldOwner(text));
            assertEquals(owner, lookup.generatedFieldOwner(companion + ".chunks.embedding.token"));
            assertEquals(owner, lookup.generatedFieldOwner(companion + ".source_only"));
            assertEquals(owner, lookup.generatedFieldOwner("text_alias"));
            assertNull(lookup.generatedFieldOwner(companion + "_other.text"));
            assertNull(lookup.generatedFieldOwner(owner));
            assertNull(lookup.generatedFieldOwner("public_field"));
            assertNull(lookup.generatedFieldOwner("unknown"));
        }
    }

    public void testInternalFieldInsideGeneratedObject() {
        MappingLookup lookup = lookup(
            List.of(
                generatingField("title", "custom_info"),
                new MockFieldMapper("custom_info.text"),
                internalField("custom_info.text._index_prefix", "custom_info.text")
            ),
            List.of(object("custom_info", false)),
            List.of()
        );
        assertEquals("custom_info.text", lookup.generatedFieldOwner("custom_info.text._index_prefix"));
        assertEquals("title", lookup.generatedFieldOwner("custom_info.text"));
        assertNull(lookup.generatedFieldOwner("title"));
    }

    public void testOwnershipUsesCurrentMappingSnapshot() {
        ObjectMapper companion = object("custom_info", false);
        MappingLookup before = lookup(List.of(new MockFieldMapper("title")), List.of(companion), List.of());
        MappingLookup after = lookup(List.of(generatingField("title", "custom_info")), List.of(companion), List.of());
        assertNull(before.generatedFieldOwner("custom_info.text"));
        assertEquals("title", after.generatedFieldOwner("custom_info.text"));
        assertNull(before.generatedFieldOwner("custom_info.text"));
    }

    public void testRejectMissingObject() {
        MapperParsingException e = expectThrows(
            MapperParsingException.class,
            () -> lookup(List.of(generatingField("title", "missing")), List.of(), List.of())
        );
        assertEquals("Generated object [missing] declared by [title] does not exist", e.getMessage());
    }

    public void testRejectConflictingOwners() {
        MapperParsingException e = expectThrows(
            MapperParsingException.class,
            () -> lookup(
                List.of(generatingField("title", "shared"), generatingField("body", "shared")),
                List.of(object("shared", false)),
                List.of()
            )
        );
        assertEquals("Generated object [shared] has multiple owners", e.getMessage());
    }

    public void testRejectOwnershipCycles() {
        expectThrows(
            MapperParsingException.class,
            () -> lookup(List.of(generatingField("generated.title", "generated")), List.of(object("generated", false)), List.of())
        );
        expectThrows(
            MapperParsingException.class,
            () -> lookup(List.of(internalField("a", "b"), internalField("b", "a")), List.of(), List.of())
        );
        expectThrows(
            MapperParsingException.class,
            () -> lookup(
                List.of(generatingField("a.title", "b"), generatingField("b.title", "a")),
                List.of(object("a", false), object("b", false)),
                List.of()
            )
        );
    }

    private static FieldMapper generatingField(String name, String object) {
        return new MockFieldMapper(new MockFieldMapper.FakeFieldType(name) {
            @Override
            public Set<String> generatedObjectPaths() {
                return Set.of(object);
            }
        });
    }

    private static FieldMapper internalField(String name, String owner) {
        return new MockFieldMapper(new MockFieldMapper.FakeFieldType(name) {
            @Override
            public String generatedFieldOwner() {
                return owner;
            }
        });
    }

    private static ObjectMapper object(String name, boolean nested) {
        return new ObjectMapper(
            name,
            name,
            new Explicit<>(true, false),
            nested ? ObjectMapper.Nested.newNested() : ObjectMapper.Nested.NO,
            ObjectMapper.Dynamic.FALSE,
            new Explicit<>(false, false),
            Map.of(),
            MockFieldMapper.DEFAULT_SETTINGS
        );
    }

    private static MappingLookup lookup(List<FieldMapper> fields, List<ObjectMapper> objects, List<FieldAliasMapper> aliases) {
        return new MappingLookup(fields, objects, aliases, 0, Lucene.STANDARD_ANALYZER);
    }
}
