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

package org.opensearch.index.mapper;

import org.apache.lucene.analysis.Analyzer;
import org.opensearch.cluster.metadata.DataStream;
import org.opensearch.common.Nullable;
import org.opensearch.common.annotation.ExperimentalApi;
import org.opensearch.common.annotation.PublicApi;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.FieldNameAnalyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Looks up a mapping for a field
 *
 * @opensearch.api
 */
@PublicApi(since = "1.0.0")
public final class MappingLookup implements Iterable<Mapper> {

    /** Full field name to mapper */
    private final Map<String, Mapper> fieldMappers;
    private final Map<String, ObjectMapper> objectMappers;
    private final boolean hasNested;
    private final boolean rootDisableObjects;
    private final FieldTypeLookup fieldTypeLookup;
    private final int metadataFieldCount;
    private final FieldNameAnalyzer indexAnalyzer;
    private final Map<String, String> generatedObjectOwners;

    private static void put(Map<String, Analyzer> analyzers, String key, Analyzer value, Analyzer defaultValue) {
        if (value == null) {
            value = defaultValue;
        }
        analyzers.put(key, value);
    }

    /**
     * Builds a lookup from a mapping, resolving dynamic_property patterns for {@link MappedFieldType} when needed.
     * Fields resolved only via dynamic_property patterns are not added to the serialized mapping; see
     * {@link DynamicProperty} for implications on hooks tied to mapping updates.
     */
    public static MappingLookup fromMapping(Mapping mapping, Analyzer defaultIndex, DocumentMapperParser documentMapperParser) {
        List<ObjectMapper> newObjectMappers = new ArrayList<>();
        List<FieldMapper> newFieldMappers = new ArrayList<>();
        List<FieldAliasMapper> newFieldAliasMappers = new ArrayList<>();
        for (MetadataFieldMapper metadataMapper : mapping.metadataMappers) {
            if (metadataMapper != null) {
                newFieldMappers.add(metadataMapper);
            }
        }
        collect(mapping.root, newObjectMappers, newFieldMappers, newFieldAliasMappers);
        DynamicPropertyFieldTypeResolver dynamicResolver = null;
        if (documentMapperParser != null && mapping.root().dynamicProperties().length > 0) {
            dynamicResolver = new DynamicPropertyFieldTypeResolver(mapping.root(), documentMapperParser);
        }
        return new MappingLookup(
            newFieldMappers,
            newObjectMappers,
            newFieldAliasMappers,
            mapping.metadataMappers.length,
            defaultIndex,
            dynamicResolver,
            mapping.root().disableObjects()
        );
    }

    public static MappingLookup fromMapping(Mapping mapping, Analyzer defaultIndex) {
        return fromMapping(mapping, defaultIndex, null);
    }

    private static void collect(
        Mapper mapper,
        Collection<ObjectMapper> objectMappers,
        Collection<FieldMapper> fieldMappers,
        Collection<FieldAliasMapper> fieldAliasMappers
    ) {
        if (mapper instanceof RootObjectMapper) {
            // root mapper isn't really an object mapper
        } else if (mapper instanceof ObjectMapper) {
            objectMappers.add((ObjectMapper) mapper);
        } else if (mapper instanceof FieldMapper) {
            fieldMappers.add((FieldMapper) mapper);
        } else if (mapper instanceof FieldAliasMapper) {
            fieldAliasMappers.add((FieldAliasMapper) mapper);
        } else {
            throw new IllegalStateException("Unrecognized mapper type [" + mapper.getClass().getSimpleName() + "].");
        }

        for (Mapper child : mapper) {
            collect(child, objectMappers, fieldMappers, fieldAliasMappers);
        }
    }

    public MappingLookup(
        Collection<FieldMapper> mappers,
        Collection<ObjectMapper> objectMappers,
        Collection<FieldAliasMapper> aliasMappers,
        int metadataFieldCount,
        Analyzer defaultIndex
    ) {
        this(mappers, objectMappers, aliasMappers, metadataFieldCount, defaultIndex, null, false);
    }

    MappingLookup(
        Collection<FieldMapper> mappers,
        Collection<ObjectMapper> objectMappers,
        Collection<FieldAliasMapper> aliasMappers,
        int metadataFieldCount,
        Analyzer defaultIndex,
        DynamicPropertyFieldTypeResolver dynamicPropertyFieldTypes,
        boolean rootDisableObjects
    ) {
        Map<String, Mapper> fieldMappers = new HashMap<>();
        Map<String, Analyzer> indexAnalyzers = new HashMap<>();
        Map<String, ObjectMapper> objects = new HashMap<>();

        boolean hasNested = false;
        for (ObjectMapper mapper : objectMappers) {
            if (objects.put(mapper.fullPath(), mapper) != null) {
                throw new MapperParsingException("Object mapper [" + mapper.fullPath() + "] is defined more than once");
            }
            if (mapper.nested().isNested()) {
                hasNested = true;
            }
        }
        this.hasNested = hasNested;
        this.rootDisableObjects = rootDisableObjects;

        for (FieldMapper mapper : mappers) {
            if (objects.containsKey(mapper.name())) {
                throw new MapperParsingException("Field [" + mapper.name() + "] is defined both as an object and a field");
            }
            if (fieldMappers.put(mapper.name(), mapper) != null) {
                throw new MapperParsingException("Field [" + mapper.name() + "] is defined more than once");
            }
            MappedFieldType fieldType = mapper.fieldType();
            put(indexAnalyzers, fieldType.name(), fieldType.indexAnalyzer(), defaultIndex);
        }
        this.metadataFieldCount = metadataFieldCount;

        for (FieldAliasMapper aliasMapper : aliasMappers) {
            if (objects.containsKey(aliasMapper.name())) {
                throw new MapperParsingException("Alias [" + aliasMapper.name() + "] is defined both as an object and an alias");
            }
            if (fieldMappers.put(aliasMapper.name(), aliasMapper) != null) {
                throw new MapperParsingException("Alias [" + aliasMapper.name() + "] is defined both as an alias and a concrete field");
            }
        }

        this.fieldTypeLookup = new FieldTypeLookup(mappers, aliasMappers, dynamicPropertyFieldTypes);

        this.fieldMappers = Collections.unmodifiableMap(fieldMappers);
        this.indexAnalyzer = new FieldNameAnalyzer(indexAnalyzers);
        this.objectMappers = Collections.unmodifiableMap(objects);
        Map<String, String> owners = new HashMap<>();
        for (FieldMapper mapper : mappers) {
            for (String path : mapper.fieldType().generatedObjectPaths()) {
                if (objects.containsKey(path) == false) {
                    throw new MapperParsingException("Generated object [" + path + "] declared by [" + mapper.name() + "] does not exist");
                }
                String previous = owners.putIfAbsent(path, mapper.name());
                if (previous != null && previous.equals(mapper.name()) == false) {
                    throw new MapperParsingException("Generated object [" + path + "] has multiple owners");
                }
            }
        }
        this.generatedObjectOwners = Map.copyOf(owners);
        for (FieldMapper mapper : mappers) {
            String field = generatedFieldOwner(mapper.name());
            if (field == null) {
                continue;
            }
            Set<String> visited = new HashSet<>();
            visited.add(mapper.name());
            while (field != null) {
                if (visited.add(field) == false) {
                    throw new MapperParsingException("Generated-field ownership cycle involving [" + field + "]");
                }
                field = generatedFieldOwner(field);
            }
        }
    }

    /**
     * Returns the immediate generating field for an internal field, generated object, or descendant,
     * or {@code null} when no owner is declared. Aliases resolve through their concrete field.
     * Descendants present only in {@code _source} are covered by generated object declarations too.
     * <p>
     * An explicitly owned internal field takes precedence over object ancestry; otherwise the nearest
     * generated object supplies the owner. Ownership may be chained: a generated prefix field belongs
     * to its text field, which may itself belong to the field that generated its containing object.
     * Consumers applying restrictions must consider the requested field and every owner in that chain.
     * This metadata does not itself grant or deny access.
     *
     * @param fullName fully qualified field or source path
     * @return the immediate owner's fully qualified name, or {@code null}
     */
    @ExperimentalApi
    @Nullable
    public String generatedFieldOwner(String fullName) {
        MappedFieldType fieldType = fieldTypeLookup.get(fullName);
        if (fieldType != null) {
            if (fieldType.generatedFieldOwner() != null) {
                return fieldType.generatedFieldOwner();
            }
            fullName = fieldType.name();
        }
        if (generatedObjectOwners.isEmpty()) {
            return null;
        }
        while (true) {
            String owner = generatedObjectOwners.get(fullName);
            if (owner != null) {
                return owner;
            }
            int dot = fullName.lastIndexOf('.');
            if (dot < 0) {
                return null;
            }
            fullName = fullName.substring(0, dot);
        }
    }

    /**
     * Returns the leaf mapper associated with this field name. Note that the returned mapper
     * could be either a concrete {@link FieldMapper}, or a {@link FieldAliasMapper}.
     * <p>
     * To access a field's type information, {@link MapperService#fieldType} should be used instead.
     */
    public Mapper getMapper(String field) {
        return fieldMappers.get(field);
    }

    FieldTypeLookup fieldTypes() {
        return fieldTypeLookup;
    }

    /**
     * A smart analyzer used for indexing that takes into account specific analyzers configured
     * per {@link FieldMapper}.
     */
    public Analyzer indexAnalyzer() {
        return this.indexAnalyzer;
    }

    @Override
    public Iterator<Mapper> iterator() {
        return fieldMappers.values().iterator();
    }

    public void checkLimits(IndexSettings settings) {
        checkFieldLimit(settings.getMappingTotalFieldsLimit());
        checkObjectDepthLimit(settings.getMappingDepthLimit());
        checkFieldNameLengthLimit(settings.getMappingFieldNameLengthLimit());
        checkNestedLimit(settings.getMappingNestedFieldsLimit());
    }

    private void checkFieldLimit(long limit) {
        if (fieldMappers.size() + objectMappers.size() - metadataFieldCount > limit) {
            throw new IllegalArgumentException("Limit of total fields [" + limit + "] has been exceeded");
        }
    }

    private void checkObjectDepthLimit(long limit) {
        for (String objectPath : objectMappers.keySet()) {
            int numDots = 0;
            for (int i = 0; i < objectPath.length(); ++i) {
                if (objectPath.charAt(i) == '.') {
                    numDots += 1;
                }
            }
            final int depth = numDots + 2;
            if (depth > limit) {
                throw new IllegalArgumentException(
                    "Limit of mapping depth [" + limit + "] has been exceeded due to object field [" + objectPath + "]"
                );
            }
        }
    }

    private void checkFieldNameLengthLimit(long limit) {
        Stream.of(objectMappers.values().stream(), fieldMappers.values().stream())
            .reduce(Stream::concat)
            .orElseGet(Stream::empty)
            .forEach(mapper -> {
                String name = mapper.simpleName();
                if (name.length() > limit) {
                    throw new IllegalArgumentException("Field name [" + name + "] is longer than the limit of [" + limit + "] characters");
                }
            });
    }

    private void checkNestedLimit(long limit) {
        long actualNestedFields = 0;
        for (ObjectMapper objectMapper : objectMappers.values()) {
            if (objectMapper.nested().isNested()) {
                actualNestedFields++;
            }
        }
        if (actualNestedFields > limit) {
            throw new IllegalArgumentException("Limit of nested fields [" + limit + "] has been exceeded");
        }
    }

    public boolean hasNested() {
        return hasNested;
    }

    public Map<String, ObjectMapper> objectMappers() {
        return objectMappers;
    }

    public boolean isMultiField(String field) {
        String sourceParent = parentObject(field);
        if (sourceParent == null || !fieldMappers.containsKey(sourceParent)) {
            return false;
        }
        // When disable_objects is true, dotted fields are independent flat fields, not multi-fields.
        // Check if the root or any ancestor ObjectMapper has disable_objects enabled.
        if (rootDisableObjects) {
            return false;
        }
        for (String ancestor = parentObject(field); ancestor != null; ancestor = parentObject(ancestor)) {
            ObjectMapper objectMapper = objectMappers.get(ancestor);
            if (objectMapper != null && objectMapper.disableObjects()) {
                return false;
            }
        }
        return true;
    }

    public boolean isObjectField(String field) {
        return objectMappers.containsKey(field);
    }

    public String getNestedScope(String path) {
        for (String parentPath = parentObject(path); parentPath != null; parentPath = parentObject(parentPath)) {
            ObjectMapper objectMapper = objectMappers.get(parentPath);
            if (objectMapper != null && objectMapper.nested().isNested()) {
                return parentPath;
            }
        }
        return null;
    }

    /**
     * If this index contains @timestamp field with Date type, it will return true
     * @return true or false based on above condition
     */
    public boolean containsTimeStampField() {
        MappedFieldType timeSeriesFieldType = this.fieldTypeLookup.get(DataStream.TIMESERIES_FIELDNAME);
        return timeSeriesFieldType != null && timeSeriesFieldType.unwrap() instanceof DateFieldMapper.DateFieldType; // has to be Date field
                                                                                                                     // type
    }

    private static String parentObject(String field) {
        int lastDot = field.lastIndexOf('.');
        if (lastDot == -1) {
            return null;
        }
        return field.substring(0, lastDot);
    }
}
