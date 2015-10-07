package com.beardandcode.forms;

import static java.util.stream.Collectors.toMap;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.SchemaLoader;
import com.github.fge.jsonschema.core.load.RefResolver;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Map;



public class SchemaWalker {

    private static final JsonPointer PROPERTIES
        = JsonPointer.of("properties");
    
    private final RefResolver resolver;
    private final ObjectMapper mapper = new ObjectMapper();

    public SchemaWalker(SchemaLoader loader) {
        this.resolver = new RefResolver(loader);
    }

    public IPersistentMap walk(final SchemaTree tree) {
        final SchemaTree resolvedTree;
        
        try {
            resolvedTree = resolver.rawProcess(null, tree);
        } catch (ProcessingException ex) {
            ex.printStackTrace();
            return null;
        }
        
        final JsonNode schemaNode = resolvedTree.getNode();
        final Map<String, Object> schema = processMap(mapper.convertValue(schemaNode, Map.class));

        if (schemaNode.path("properties").isObject()) {
            final Iterator<String> fields = schemaNode.path("properties").fieldNames();
            final Map<String, IPersistentMap> children = Lists.newArrayList(fields).stream()
                .collect(toMap(prop -> prop, prop -> walk(resolvedTree.append(PROPERTIES.append(prop)))));
            
            schema.put("properties", PersistentHashMap.create(children));
        }
        
        return PersistentHashMap.create(schema);
    }

    private Map<String, Object> processMap(final Map<String, Object> map) {
        return map.entrySet().stream()
            .filter(e -> !e.getKey().equals("properties"))
            .map(e -> {
                    Object value = e.getValue();
                    if (value instanceof Map) {
                        e.setValue(PersistentHashMap.create(processMap((Map<String, Object>) value)));
                    } else if (value instanceof List) {
                        e.setValue(PersistentVector.create((List) value));
                    }
                    return e;
                })
            .collect(toMap(e -> e.getKey(), e -> e.getValue()));
    }
    
}
