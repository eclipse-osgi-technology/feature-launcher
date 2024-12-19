package com.kentyou.featurelauncher.common.repository.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.common.util.impl.FileSystemUtil;
import com.kentyou.featurelauncher.repository.spi.FileSystemArtifactRepository;
import com.kentyou.featurelauncher.repository.spi.NamedArtifactRepository;

public class WrappingArtifactRepository implements NamedArtifactRepository, FileSystemArtifactRepository {

	private static final Logger LOG = LoggerFactory.getLogger(WrappingArtifactRepository.class);
	
	private final ArtifactRepository wrapped;
	
	private final Path localRepoPath;
	
	private final String name;
	
	public WrappingArtifactRepository(ArtifactRepository toWrap, String name) {
		Objects.requireNonNull(toWrap, "A repository must be supplied for wrapping");
		this.wrapped = toWrap;

		if(this.wrapped instanceof NamedArtifactRepository nar) {
			this.name = nar.getName();
		} else {
			Objects.requireNonNull(name, "A name must be supplied for the repository being wrapped as it is not named");
			this.name = name;
		}

		if(this.wrapped instanceof FileSystemArtifactRepository) {
			localRepoPath = null;
		} else {
			localRepoPath = createTemporaryFolder();
		}
	}
	
	private Path createTemporaryFolder() {
		try {
			Path localRepositoryPath = Files.createTempDirectory("featurelauncherM2repo_");

			deleteOnShutdown(localRepositoryPath);

			return localRepositoryPath;

		} catch (IOException e) {
			throw new IllegalStateException("Could not create temporary local artifact repository!", e);
		}
	}
	
	private void deleteOnShutdown(Path localRepositoryPath) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					FileSystemUtil.recursivelyDelete(localRepositoryPath);
				} catch (IOException e) {
					LOG.warn("Could not delete temporary local artifact repository!");
				}
			}
		});
	}
	
	@Override
	public InputStream getArtifact(ID id) {
		return wrapped.getArtifact(id);
	}

	@Override
	public Path getArtifactPath(ID id) {
		if(localRepoPath != null) {
			return ((FileSystemArtifactRepository)wrapped).getArtifactPath(id);
		} else {
			InputStream is = getArtifact(id);
			if(is == null) {
				return null;
			}
			try(is) {
				Path p = localRepoPath.resolve(id.getGroupId())
						.resolve(id.getArtifactId())
						.resolve(id.getVersion());
				
				Files.createDirectories(p);
				
				String fileName = "file" + 
						id.getClassifier().map(c -> "-" + c).orElse("") +
						id.getType().orElse("jar");

				p = p.resolve(fileName);
				try(OutputStream os = Files.newOutputStream(p)) {
					is.transferTo(os);
				}
				return p;
			} catch(IOException ioe) {
				LOG.error("Failed caching artifact {}", id);
			}
		}
		return null;
	}

	@Override
	public Path getLocalRepositoryPath() {
		if(localRepoPath != null) {
			return ((FileSystemArtifactRepository)wrapped).getLocalRepositoryPath();
		} else {
			return localRepoPath;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Wrapped Artifact repository: Name = " + name + " repo = " + wrapped;
	}
}
