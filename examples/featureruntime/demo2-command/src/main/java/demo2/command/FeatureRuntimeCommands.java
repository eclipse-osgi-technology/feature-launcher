package demo2.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ComponentPropertyType;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;

@FeatureRuntimeCommands.Commands(scope="featureruntime", function= {"list", "addFeature", "removeFeature"})
@Component(service=FeatureRuntimeCommands.class)
public class FeatureRuntimeCommands {
	
	@Reference
	FeatureRuntime featureRuntime;
	
	public String list() {
		return featureRuntime.getInstalledFeatures()
			.stream()
			.map(this::prettyPrintInstalledFeature)
			.collect(Collectors.joining(",\n", "Installed Features:\n", ""));
	}
	
	private String prettyPrintInstalledFeature(InstalledFeature f) {
		StringBuilder sb = new StringBuilder();
		sb.append(f.getFeature().getID())
			.append(" ::\n  Installed Bundles:\n");
			
		f.getInstalledBundles().stream()
			.map(ib -> ib.getBundleId())
			.forEach(id -> sb.append("    ")
						.append(id)
						.append("\n"));
		
		sb.append("  Installed Configurations:\n");

		f.getInstalledConfigurations().stream()
		.forEach(ic -> sb.append("    ")
				.append(ic.getPid())
				.append("\n")
				.append("      ")
				.append(ic.getProperties().entrySet().stream()
						.map(Object::toString)
						.collect(Collectors.joining("      ", "\n      ", "\n"))));
		
		return sb.toString();
	}
	
	public String addFeature(String feature) throws IOException {
		
		BufferedReader json = Files.newBufferedReader(Paths.get("target/features", feature));
		
		InstalledFeature installedFeature = featureRuntime.install(json)
				.useDefaultRepositories(false)
				.addRepository("local", featureRuntime.createRepository(Paths.get("target/repo")))
				.install();
		
		return prettyPrintInstalledFeature(installedFeature);
	}
	
	public void removeFeature(String feature) {
		InstalledFeature i = featureRuntime.getInstalledFeatures().stream()
			.filter(ib -> ib.getFeature().getID().toString().equals(feature))
			.findFirst()
			.orElseThrow();
		featureRuntime.remove(i.getFeature().getID());
	}

	
	@ComponentPropertyType
	public static @interface Commands {
		public static final String PREFIX_ = "osgi.command.";
		String scope();
		String[] function();
	}
}
