package com.redhat.vertx.pipeline.templates;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.interpret.TemplateSyntaxException;
import com.hubspot.jinjava.lib.filter.AdvancedFilter;
import com.hubspot.jinjava.lib.fn.ELFunction;
import com.hubspot.jinjava.lib.filter.Filter;
import org.kohsuke.MetaInfServices;

import java.util.List;
import java.util.Map;
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
            if (var == null) {
                return null;
                // throw new InvalidInputException(interpreter,getName(),getName() + " pipe input cannot be null");
            }
            if (args.length != 1) {
                throw new TemplateSyntaxException(interpreter, getName(), "requires 1 argument (the pattern)");
            }
            return match((String) var, args[0]);
        }

        @Override
        public String getName() {
            return "regex_search";
        }
    }

    @MetaInfServices(Filter.class)
    public static class ReplaceFilter implements AdvancedFilter {

        @Override
        public Object filter(Object var, JinjavaInterpreter interpreter, Object[] args, Map kwargs) {
            if (var == null) {
                return null;
            }
            if (args.length == 1 && (args[0] instanceof List)) {
                args = ((List<String>) args[0]).toArray();
            }
            if (args.length != 2) {
                throw new TemplateSyntaxException(interpreter, getName(), "requires 2 arguments (pattern, new_content)");
            }
            assert args.length == 2;
            return substitute((String)var, (String)args[0], (String)args[1]);
        }

        @Override
        public String getName() {
            return "regex_replace";
        }
    }
}
