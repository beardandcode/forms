package com.beardandcode.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonNumEquals;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.AbstractSyntaxChecker;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.SyntaxChecker;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.google.common.base.Equivalence;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;

public class OrderSyntaxChecker extends AbstractSyntaxChecker {
    private static final Equivalence<JsonNode> EQUIVALENCE
        = JsonNumEquals.getInstance();

    private static final SyntaxChecker INSTANCE = new OrderSyntaxChecker();

    public static SyntaxChecker getInstance()
    {
        return INSTANCE;
    }

    private OrderSyntaxChecker()
    {
        super("order", NodeType.ARRAY);
    }
    
    @Override
    protected void checkValue(final Collection<JsonPointer> pointers,
                              final MessageBundle bundle, final ProcessingReport report,
                              final SchemaTree tree)
        throws ProcessingException
    {
        final Set<Equivalence.Wrapper<JsonNode>> set = Sets.newHashSet();

        for (final JsonNode element: getNode(tree))
            if (!set.add(EQUIVALENCE.wrap(element))) {
                report.error(newMsg(tree, bundle,
                                    "common.array.duplicateElements"));
                return;
            }
    }
}
