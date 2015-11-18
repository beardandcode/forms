package com.beardandcode.forms;

import com.github.fge.jackson.NodeType;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

public final class PasswordFormatAttribute extends AbstractFormatAttribute {

    private static final FormatAttribute instance = new PasswordFormatAttribute();

    private PasswordFormatAttribute()
    {
        super("password", NodeType.STRING);
    }

    public static FormatAttribute getInstance()
    {
        return instance;
    }

    @Override
    public void validate(final ProcessingReport report,
                         final MessageBundle bundle, final FullData data)
                throws ProcessingException
    {}
    
}
