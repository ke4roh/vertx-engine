package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.lib.fn.ELFunction;
import org.kohsuke.MetaInfServices;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MetaInfServices(JinjaFunctionDefinition.class)
public class JinjaRe extends AbstractJinjaFunctionDefinition {

    @ELFunction(value = "m", namespace = "re")
    public static String match(String s, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(s);
        return m.matches() ? m.group(0) : null;
    }
}
