package demo1;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ServiceLoader;

import org.osgi.framework.launch.Framework;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

public class API_Launcher {

	public static void main(String[] args) throws Exception {

		ServiceLoader<FeatureLauncher> loader = ServiceLoader.load(FeatureLauncher.class);
		FeatureLauncher launcher = loader.findFirst()
			.orElseThrow(() -> new ClassNotFoundException("No Feature Launcher implementation found"));

		Path targetDir = Paths.get("target");
		
		ArtifactRepository repository = launcher.createRepository(
				URI.create("https://repo.maven.apache.org/maven2/"),
				new HashMap<>()
		);

		BufferedReader feature = Files.newBufferedReader(targetDir.resolve("features/gogo.json"));
		Framework launchFramework = launcher.launch(feature)
			.withRepository(repository)
			.launchFramework();
		
		launchFramework.waitForStop(0);
	}
}
