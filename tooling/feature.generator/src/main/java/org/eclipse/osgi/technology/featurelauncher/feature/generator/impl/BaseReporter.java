package org.eclipse.osgi.technology.featurelauncher.feature.generator.impl;

import java.util.List;

import aQute.service.reporter.Reporter;

public class BaseReporter implements Reporter {

	@Override
	public List<String> getWarnings() {
		return List.of();
	}

	@Override
	public List<String> getErrors() {
		return List.of();
	}

	@Override
	public Location getLocation(String msg) {
		return null;
	}

	@Override
	public boolean isOk() {
		return true;
	}

	@Override
	public SetLocation error(String format, Object... args) {
		return null;
	}

	@Override
	public SetLocation warning(String format, Object... args) {
		return null;
	}

	@Override
	public void trace(String format, Object... args) {

	}

	@Override
	public void progress(float progress, String format, Object... args) {

	}

	@Override
	public SetLocation exception(Throwable t, String format, Object... args) {
		return null;
	}

	@Override
	public boolean isPedantic() {
		return false;
	}

}
