package demo2;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

public class API_Launcher {

	public static void main(String[] args) throws Exception {
		
		ServiceLoader<FeatureLauncher> loader = ServiceLoader.load(FeatureLauncher.class);
		FeatureLauncher launcher = loader.findFirst()
			.orElseThrow(() -> new ClassNotFoundException("No Feature Launcher implementation found"));

		Path targetDir = Paths.get("target");

		ArtifactRepository localRepo = launcher.createRepository(
				Paths.get(System.getProperty("user.home"), ".m2", "repository"));

		ArtifactRepository repositoryMaven = launcher.createRepository(
				URI.create("https://repo.maven.apache.org/maven2/"),
				Map.of()
		);

		ArtifactRepository repositorySonatypeSnapshots = launcher.createRepository(
				URI.create("https://oss.sonatype.org/content/repositories/snapshots/"),
				Map.of()
		);

		BufferedReader feature = Files.newBufferedReader(targetDir.resolve("features/gogo.json"));
		Framework launchFramework = launcher.launch(feature)
			.withRepository(localRepo)
			.withRepository(repositoryMaven)
			.withRepository(repositorySonatypeSnapshots)
			.withFrameworkProperties(Map.of(Constants.FRAMEWORK_STORAGE_CLEAN,
					Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT))
			.launchFramework();
		
		launchFramework.waitForStop(0);
	}
}