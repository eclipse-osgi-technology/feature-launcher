package org.eclipse.osgi.technology.featurelauncher.feature.generator.impl.pebble;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class IsStringFunction implements Function {

    @Override
    public List<String> getArgumentNames() {
        return List.of("object");
    }

    @Override
    public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        Object obj = args.get("object");

        return obj instanceof String;
    }
}