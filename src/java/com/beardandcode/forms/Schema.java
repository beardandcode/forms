package com.beardandcode.forms;

import java.io.IOException;
import java.io.File;

import clojure.lang.IPersistentMap;
import clojure.lang.ISeq;
import clojure.lang.RT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.jsonpointer.JsonPointer;

import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.SchemaLoader;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.core.util.Dictionary;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.library.Keyword;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.collect.Lists;

public class Schema {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMA_FACTORY;

    static {
        ValidationConfiguration validationConfiguration = ValidationConfiguration.byDefault();
        Library defaultLib = validationConfiguration.getDefaultLibrary();
        Dictionary<FormatAttribute> formatAttributes = defaultLib.getFormatAttributes();

        defaultLib = defaultLib.thaw()
            .addFormatAttribute("password", PasswordFormatAttribute.getInstance())
            .addKeyword(Keyword.newBuilder("order").withSyntaxChecker(OrderSyntaxChecker.getInstance()).freeze())
            .addKeyword(Keyword.newBuilder("submit").withSyntaxChecker(SubmitSyntaxChecker.getInstance()).freeze())
            .freeze();
        validationConfiguration = validationConfiguration.thaw().setDefaultLibrary("http://forms.beardandcode.com/draft-04/schema#", defaultLib).freeze();

        SCHEMA_FACTORY = JsonSchemaFactory.newBuilder().setValidationConfiguration(validationConfiguration).freeze();
    }

    private final SchemaLoader loader = new SchemaLoader();
    private final JsonNode node;
    private final SchemaTree tree;
    private final JsonSchema schema;

    public Schema(final File schemaFile) throws IOException, ProcessingException {
        node = MAPPER.readTree(schemaFile);
        tree = loader.load(node).setPointer(JsonPointer.empty());
        schema = SCHEMA_FACTORY.getJsonSchema(node);
    }

    public boolean isValid() {
        return SCHEMA_FACTORY.getSyntaxValidator().schemaIsValid(node);
    }

    public IPersistentMap asMap() {
        return new SchemaWalker(loader).walk(tree);
    }

    public String validate(String representation) throws IOException, ProcessingException {
        JsonNode instance = JsonLoader.fromString(representation);
        ListProcessingReport report = new ListProcessingReport();
        
        report.mergeWith(schema.validate(instance));
        
        return report.asJson().toString();
    }
    
}
