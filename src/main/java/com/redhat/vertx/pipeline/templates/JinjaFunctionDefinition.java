package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;

import java.util.Collection;

public interface JinjaFunctionDefinition {
    Collection<ELFunctionDefinition> getFunctionDefinition();
}
