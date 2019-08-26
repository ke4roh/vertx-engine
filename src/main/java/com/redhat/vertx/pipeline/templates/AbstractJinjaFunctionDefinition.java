package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.lib.fn.ELFunction;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class AbstractJinjaFunctionDefinition implements JinjaFunctionDefinition {
    @Override
    public Collection<ELFunctionDefinition> getFunctionDefinition() {
        return Arrays.asList(this.getClass().getDeclaredMethods()).stream()
                .filter(f -> (f.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC) ) != 0 &&
                        f.getAnnotation(ELFunction.class) != null)
                .map(f -> new ELFunctionDefinition(
                        f.getAnnotation(ELFunction.class).namespace(),
                        f.getAnnotation(ELFunction.class).value(),
                        f))
                .collect(Collectors.toList());
    }

}
