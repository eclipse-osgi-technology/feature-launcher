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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodExitRequest;

public class SimpleCliLaunchTest2 {

	protected Path localM2RepositoryPath;

	@TempDir
	Path tmp;

	@BeforeEach
	void setupRepoPath() throws Exception {
		this.localM2RepositoryPath = getLocalRepoPath();

		if (!Files.isDirectory(localM2RepositoryPath)
				|| !Files.newDirectoryStream(localM2RepositoryPath).iterator().hasNext()) {
			throw new IllegalStateException("Local Maven repository does not exist!");
		}
	}

	protected Path getLocalRepoPath() throws Exception {
		// Obtain path of dedicated local Maven repository
		return Paths.get(System.getProperty("localRepositoryPath", "target/m2Repo"));
	}

	protected Path getLauncherJar() throws Exception {
		Path p = Paths.get(System.getProperty("project.build.directory", "target"));

		return Files.list(p).filter(file -> {
			String s = file.getFileName().toString();
			if (s.endsWith(".jar")) {
				try (JarFile jar = new JarFile(file.toFile())) {
					return jar.getManifest().getMainAttributes().containsKey(new Name("Main-Class"));
				} catch (Exception e) {
					throw new RuntimeException("Unable to open Jar file", e);
				}
			}
			return false;
		}).findFirst().get();
	}

	@Test
	public void testBasicLaunch() throws Exception {
		Path path = tmp.resolve("h2.json");
		try (OutputStream os = Files.newOutputStream(path);
				InputStream is = getClass().getClassLoader()
						.getResourceAsStream("features/h2-feature.json")) {
			is.transferTo(os);
		}

		Path java = Paths.get(System.getProperty("java.home"), "bin", "java");

		String port = "5018";
		ProcessBuilder builder = new ProcessBuilder(java.toAbsolutePath().toString(),
				"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + port, "-jar",
				getLauncherJar().toString(), "-a", localM2RepositoryPath.toUri().toString(), "-f",
				path.toAbsolutePath().toString());
		Process p = builder.start();
		boolean started=true;
	    
//	    Writer output = new OutputStreamWriter(System.out);
//	    Writer errOutput = new OutputStreamWriter(System.err);
//	    BufferedReader reader = p.inputReader();
//	    BufferedReader errReader = p.errorReader();
//	    BufferedWriter writer = p.outputWriter();
//	    
//				reader.transferTo(output);
//    			errReader.transferTo(errOutput);
//	    
//    	try {
//    		char[] c = new char[4096];
//    		for(int i = 0; i < 100; i++) {
//    			while(reader.ready()) {
//    				output.write(c, 0, reader.read(c));
//    			}
//    			while(errReader.ready()) {
//    				errOutput.write(c, 0, errReader.read(c));
//    			}
//    			
//    			writer.write("\n");
//    			writer.flush();
//    			int read = reader.read(c, 0, 2);
//    			if(read == 2 && "g!".equals(new String(c, 0, 2))) {
//    				started = true;
//    				break;
//    			} else if (read > 0){
//    				output.write(c, 0, read);
//    			}
//    			if(p.isAlive()) {
//    				Thread.sleep(100);
//    			} else if(i < 98) {
//    				i = 98;
//    			}
//    		}
//    	} catch (Exception e) {
//e.printStackTrace();
//    		p.destroyForcibly();
//    	}

		String hostname = "localhost";

		// 1) Passenden AttachingConnector (Socket) besorgen
		AttachingConnector socketConnector = null;
		for (AttachingConnector c : Bootstrap.virtualMachineManager().attachingConnectors()) {
			if (c.getClass().getName().endsWith("SocketAttachingConnector")) {
				socketConnector = c;
				break;
			}
		}
		if (socketConnector == null) {
			throw new IllegalStateException("no dt_socket AttachingConnector found.");
		}

		Map<String, Connector.Argument> defaultArgs = socketConnector.defaultArguments();
//		defaultArgs.get("pid").setValue(pid+"");
		defaultArgs.get("timeout").setValue("10000");
		defaultArgs.get("hostname").setValue(hostname);
		defaultArgs.get("port").setValue(port);

		VirtualMachine vm = socketConnector.attach(defaultArgs);
		System.out.println("Connected to OSGi VM: " + vm.name() + " (Version " + vm.version() + ")");
		try {

			readOSGi(vm);
		} catch (Exception e) {
			e.printStackTrace();
			// p.destroyForcibly();
		}
	}

	private void readOSGi(VirtualMachine vm) throws InterruptedException {
		com.sun.jdi.ThreadReference threadRef = findThreadSuspended(vm);

		ReferenceType frameworkUtilClass = vm.classesByName("org.osgi.framework.FrameworkUtil").get(0);

		// 1) ClassType holen
		ClassType frameworkUtilClassType = (ClassType) frameworkUtilClass;

		// 2) getBundle(Class<?>)-Methode ermitteln
		Method getBundleMethod = frameworkUtilClass.methodsByName("getBundle").stream().filter(
				m -> m.argumentTypeNames().size() == 1 && m.argumentTypeNames().get(0).equals("java.lang.Class"))
				.findFirst().orElseThrow(() -> new RuntimeException("getBundle(Class) not found"));

		// 3) Beliebigen Thread auswählen, der suspendiert ist

		try {
			// 4) Argument aufbauen, also eine ClassObjectReference, z.B. so:
//			    (Falls du "BundleContext.class" oder was auch immer übergeben willst)
			ReferenceType someClassRef = vm.classesByName("org.osgi.framework.BundleContext").get(0);
			ClassObjectReference someClassObj = someClassRef.classObject();

			// 5) Statische Methode auf dem ClassType aufrufen
			Value result = frameworkUtilClassType.invokeMethod(threadRef, getBundleMethod, List.of(someClassObj), // Parameter
					0 // InvocationOptions
			);
			System.out.println("Got a BundleContext reference: " + result);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
			e.printStackTrace();
//				throw new RuntimeException(e);
		}
		System.out.println("f");
	}

	private ThreadReference findThreadSuspended(VirtualMachine vm) throws InterruptedException {

		// 5) MethodExitRequest erzeugen
//		vm.setDebugTraceMode(VirtualMachine.TRACE_ALL);
		EventRequestManager erm = vm.eventRequestManager();
		MethodExitRequest exitRequest = erm.createMethodExitRequest();
		
//		org.eclipse.osgi.technology.featurelauncher.launch.launcher.FeatureLauncherImpl.LaunchBuilderImpl
		exitRequest.addClassFilter("org.osgi.*");

		exitRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
		exitRequest.enable();

		// 6) VM weiterlaufen lassen
		vm.resume();

		// 7) Event-Loop: Warte auf MethodExitEvent
		EventQueue queue = vm.eventQueue();
		while (true) {
			EventSet eventSet = queue.remove(); // blockierend
			System.out.println(eventSet);
			for (Event event : eventSet) {
				System.out.println("--");
				System.out.println(event);
				if (event instanceof MethodExitEvent mee) {
					// Name der Methode
					String methodName = mee.location().method().name();
					System.out.println(methodName);
					if ("launchFramework".equals(methodName)) {
						// Prüfen, ob die Klasse das Interface FeatureLauncher implementiert

						ReferenceType declaringType = mee.location().declaringType();
						boolean implementsFeatureLauncher = implementsInterface(declaringType,
								"org.osgi.service.featurelauncher.FeatureLauncher");

						if (implementsFeatureLauncher) {
							System.out.println("[JDI] launchFramework() (FeatureLauncher) wird jetzt verlassen!");
							ThreadReference thread = mee.thread();

							// Thread ist suspendiert => Frames inspizieren
							try {
								List<com.sun.jdi.StackFrame> frames = thread.frames();
								for (int i = 0; i < frames.size(); i++) {
									Location loc = frames.get(i).location();
									System.out.printf("   Frame %d -> %s.%s() [line=%d]%n", i,
											loc.declaringType().name(), loc.method().name(), loc.lineNumber());
								}
							} catch (IncompatibleThreadStateException e) {
								e.printStackTrace();
							}
							return thread;
							// Ggf. invokeMethod() oder Thread weitermachen lassen
							// thread.resume(); // Hebt nur die Suspendierung für diesen Thread auf.
						}
					}
				}
			}
			// Andere Threads/Ereignisse fortsetzen
			System.out.println("res eventset");
			eventSet.resume();
//            vm.resume();

		}
	}

	private static boolean implementsInterface(ReferenceType type, String interfaceName) {
	    if (type == null) return false;


	    if (type instanceof ClassType ct) {
	        List<InterfaceType> ifs=  ct.interfaces();
	        for (InterfaceType interfaceType : ifs) {
	           if( interfaceType.name().equals(interfaceName)) {
	               return true;
	           }
                
            }
	        ClassType superClass = ct.superclass();
	        if (superClass != null) {
	            return implementsInterface(superClass, interfaceName);
	        }
	    }

	    return false;
	}

	private String getTestError(StringWriter output, StringWriter errOutput) {
		return "Error: " + errOutput.toString() + "\n\n\n" + "Output: " + output.toString();
	}
	
}
