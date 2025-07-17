package org.eclipse.osgi.technology.featurelauncher.feature.generator.impl.pebble;

import java.util.Map;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Function;

public class PebbleExtension extends AbstractExtension {
	@Override
	public Map<String, Function> getFunctions() {
		return Map.of(//
		        "isString", new IsStringFunction(), //
		        "indent", new IndentFunction()//
		);
	}
}