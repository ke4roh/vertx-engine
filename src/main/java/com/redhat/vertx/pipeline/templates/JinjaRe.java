package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.fn.ELFunction;
import com.hubspot.jinjava.lib.filter.Filter;
import org.kohsuke.MetaInfServices;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MetaInfServices(JinjaFunctionDefinition.class)
public class JinjaRe implements JinjaFunctionDefinition {

    @ELFunction(value = "m", namespace = "re")
    public static String match(String s, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(s);
        return m.find() ? m.group(0) : "";
    }

    @ELFunction(value = "s", namespace = "re")
    public static String substitute(String s, String pattern, String replacement) {
        return s.replaceAll(pattern,replacement);
    }

    @MetaInfServices(Filter.class)
    public static class MatchFilter implements Filter {

        @Override
        public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
            assert args.length == 1;
            return match((String) var, args[0]);
        }

        @Override
        public String getName() {
            return "regex_search";
        }
    }

    @MetaInfServices(Filter.class)
    public static class ReplaceFilter implements Filter {

        @Override
        public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
            assert args.length == 2;
            return substitute((String)var, args[0], args[1]);
        }

        @Override
        public String getName() {
            return "regex_replace";
        }
    }
}
