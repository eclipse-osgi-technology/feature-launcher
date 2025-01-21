/**
 * Copyright (c) 2024 Kentyou and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Kentyou - initial implementation
 */
package org.eclipse.osgi.technology.featurelauncher.launcher.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SimpleCliLaunchTest {

	protected Path localM2RepositoryPath;
	
	@TempDir
	Path tmp;
	
	@BeforeEach
	void setupRepoPath() throws Exception {
		this.localM2RepositoryPath = getLocalRepoPath();
		
		if (!Files.isDirectory(localM2RepositoryPath) || 
				!Files.newDirectoryStream(localM2RepositoryPath).iterator().hasNext()) {
			throw new IllegalStateException("Local Maven repository does not exist!");
		}
	}
	
	protected Path getLocalRepoPath() throws Exception {
		// Obtain path of dedicated local Maven repository
		return Paths.get(System.getProperty("localRepositoryPath", 
				"target/m2Repo"));
	}
	
	protected Path getLauncherJar() throws Exception {
		Path p = Paths.get(System.getProperty("project.build.directory", "target"));
		
		return Files.list(p).filter(file -> {
			String s = file.getFileName().toString();
			return s.endsWith(".jar") && !s.endsWith("-tests.jar") && !s.startsWith("original-"); 
		}).findFirst().get();
	}
	
	@Test
	public void testBasicLaunch() throws Exception {
		Path path = tmp.resolve("gogo.json");
		try (OutputStream os = Files.newOutputStream(path);
				InputStream is = getClass().getClassLoader()
						.getResourceAsStream("features/gogo-console-feature.json")) {
			is.transferTo(os);
		}
		
		Path java = Paths.get(System.getProperty("java.home"), "bin", "java");

	    ProcessBuilder builder = new ProcessBuilder(java.toAbsolutePath().toString(),
	    		"-jar", getLauncherJar().toString(),
	    		"-a", localM2RepositoryPath.toUri().toString(),
	    		"-f", path.toAbsolutePath().toString());
	    Process p = builder.start();
	    
	    boolean started = false;
	    
	    StringWriter output = new StringWriter();
	    StringWriter errOutput = new StringWriter();
	    BufferedReader reader = p.inputReader();
	    BufferedReader errReader = p.errorReader();
	    BufferedWriter writer = p.outputWriter();
	    char[] c = new char[4096];
	    for(int i = 0; i < 100; i++) {
	    	while(reader.ready()) {
	    		output.write(c, 0, reader.read(c));
	    	}
	    	while(errReader.ready()) {
	    		errOutput.write(c, 0, errReader.read(c));
	    	}
	    	
	    	writer.write("\n");
	    	writer.flush();
	    	int read = reader.read(c, 0, 2);
	    	if(read == 2 && "g!".equals(new String(c, 0, 2))) {
	    		started = true;
	    		break;
	    	} else if (read > 0){
	    		output.write(c, 0, read);
	    	}
	    	if(p.isAlive()) {
	    		Thread.sleep(100);
	    	} else if(i < 98) {
	    		i = 98;
	    	}
	    }
	    
	    assertTrue(started, () -> getTestError(output, errOutput));
	    
	    writer.write("stop 0\n");
	    writer.flush();
	    
	    if(p.waitFor(10, TimeUnit.SECONDS)) {
	    	assertEquals(0, p.exitValue(), () -> { 
	    		try {
					reader.transferTo(output);
				} catch (IOException e) {
				}
	    		try {
	    			errReader.transferTo(errOutput);
	    		} catch (IOException e) {
	    		}
	    		return getTestError(output, errOutput);
	    	});
	    } else {
	    	try {
				reader.transferTo(output);
			} catch (IOException e) {
			}
	    	try {
	    		errReader.transferTo(errOutput);
	    	} catch (IOException e) {
	    	}
	    	fail("Error: " + getTestError(output, errOutput));
	    }
	}

	private String getTestError(StringWriter output, StringWriter errOutput) {
		return "Error: " + errOutput.toString() + "\n\n\n" + "Output: " + output.toString();
	}
	
}
