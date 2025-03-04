package org.eclipse.osgi.technology.featurelauncher.extensions.hash.checker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class BundleHashChecker implements FeatureExtensionHandler {
	
	/**
	 * The recommended extension name for this extension handler
	 */
	public static final String HASH_CHECKER_EXTENSION_NAME = "eclipse.osgi.technology.hash.checker"; 

	public static final String ALLOW_INVALID_HASH_DEFINITIONS = "allow_invalid_definitions";
	public static final String ALLOW_UNVERIFIED = "allow_unverified";
	public static final String ALLOW_HASH_MISMATCH = "allow_mismatch"; 
	public static final String FORBID_UNKNOWN_ALGORITHM = "forbid_unknown"; 
	public static final String REQUIRE_MATCH = "require_match"; 
	public static final String ALL = "all"; 
	public static final String AT_LEAST_ONE = "at_least_one"; 
	public static final String ZERO_OR_MORE = "zero_or_more"; 

	public static final String HASH_CHECKER_DIGESTS = HASH_CHECKER_EXTENSION_NAME + ".digests"; 

	private static final Logger LOG = LoggerFactory.getLogger(BundleHashChecker.class);
	
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			List<ArtifactRepository> repositories,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {
		if(!HASH_CHECKER_EXTENSION_NAME.equals(extension.getName())) {
			LOG.warn("The recommended extension name for using the artifact hash checker is {}, but it is being called for extensions named {}");
		}
		
		if(extension.getType() != Type.JSON) {
			LOG.error("The artifact hash checker requires JSON configuration not {}", extension.getType());
			throw new AbandonOperationException("The configuration of the artifact hash checker feature extension must be JSON.");
		}
		
		JsonObject config;
		String json = extension.getJSON();
		if(json == null || json.isBlank()) {
			config = Json.createObjectBuilder().build();
		} else {
			try (JsonReader reader = Json.createReader(new StringReader(json))) {
				config = reader.readObject();
			};
		}

		for (FeatureBundle fb : feature.getBundles()) {
			Map<String,Object> metadata = fb.getMetadata();
			
			if(!metadata.containsKey(HASH_CHECKER_DIGESTS)) {
				if(isEnabled(config, ALLOW_UNVERIFIED, true)) {
					LOG.warn("No hash validation for feature bundle {}", fb.getID());
					continue;
				} else {
					LOG.error("No hash validation for feature bundle {}", fb.getID());
					throw new AbandonOperationException("The feature bundle " + fb.getID()
							+ " contained no digest information.");
				}
			}
			
			ArtifactRepository repo = repositories.stream()
				.filter(r -> r.getArtifact(fb.getID()) != null)
				.findFirst()
				.orElseThrow(() -> new AbandonOperationException("Unable to locate feature bundle " 
						+ fb.getID() + " in a repository"));
			
			String hashes = String.valueOf(metadata.get(HASH_CHECKER_DIGESTS));
			String[] split = hashes.split(",");
			
			try {
				Stream<String[]> checks = Arrays.stream(split)
						.map(s -> validateHashDefinition(fb, s, config))
						.filter(Objects::nonNull);
				
				Predicate<String[]> check = s -> verify(fb, repo, s[0], s[1], config);
				
				boolean result;
				String matchType = getValue(config, REQUIRE_MATCH, AT_LEAST_ONE);
				switch(matchType) {
					case ALL:
						result = checks.allMatch(check);
						break;
					case AT_LEAST_ONE:
						result = checks.anyMatch(check);
						break;
					case ZERO_OR_MORE:
						checks.forEach(check::test);
						result = true;
						break;
					default:
						LOG.error("Invalid value {} for {}", matchType, REQUIRE_MATCH);
						throw new AbandonOperationException("Invalid configuration for the hash checker");
				}
				if(!result) {
					LOG.error("The artifact {} failed hash checking.", fb.getID());
					throw new AbandonOperationException("Hash checking failed for feature " + feature.getID());
				}
			} catch (HashCheckerException re) {
				throw re.getWrapped();
			}
		} 
		
		return feature;
	}

	private boolean isEnabled(JsonObject config, String key, boolean defaultValue) {
		if(config.containsKey(key)) {
			return config.getBoolean(key);
		} else {
			return defaultValue;
		}
	}

	private String getValue(JsonObject config, String key, String defaultValue) {
		if(config.containsKey(key)) {
			return config.getString(key);
		} else {
			return defaultValue;
		}
	}
	
	private String[] validateHashDefinition(FeatureBundle fb, String hashDefinition, JsonObject config) {
		String[] def = hashDefinition.split(";");
		if(def.length != 2 || def[0].isBlank() || def[1].isBlank()) {
			LOG.warn("Feature bundle {} declares an invalid hash definition {}", fb, hashDefinition);
			if(isEnabled(config, ALLOW_INVALID_HASH_DEFINITIONS, false)) {
				return null;
			} else { 
				throw new HashCheckerException("Invalid hash definition " + hashDefinition);
			}
		}
		return def;
	}
	
	private boolean verify(FeatureBundle fb, ArtifactRepository repo, String hashFunction, String signature, JsonObject config) {
		
		try {
			MessageDigest digest = MessageDigest.getInstance(hashFunction);
			repo.getArtifact(fb.getID()).transferTo(
					new DigestOutputStream(OutputStream.nullOutputStream(), digest));
			
			String calculated = HexFormat.of().formatHex(digest.digest());
			if(signature.equalsIgnoreCase(calculated)) {
				return true;
			} else {
				if(isEnabled(config, ALLOW_HASH_MISMATCH, false)) {
					LOG.warn("The {} hash for {} did not match. Expected \n\n{}\n\n but was\n\n {}",
							hashFunction, fb.getID(), signature, calculated);
					return false;
				} else {
					LOG.error("The {} hash for {} did not match. Expected \n\n{}\n\n but was\n\n {}",
							hashFunction, fb.getID(), signature, calculated);
					throw new HashCheckerException("The " + hashFunction + " hash for " 
							+ fb.getID() + " did not match");
				}
			}
		} catch (NoSuchAlgorithmException e) {
			if(isEnabled(config, FORBID_UNKNOWN_ALGORITHM, false)) {
				LOG.error("The {} hash algorithm is not available", hashFunction);
				throw new HashCheckerException("The " + hashFunction + " is not available", e);
			} else {
				LOG.warn("The {} hash algorithm is not available", hashFunction);
				return false;
			}
		} catch (IOException e) {
			LOG.error("An unexpected error occurred", hashFunction);
			throw new HashCheckerException(e.getMessage(), e);
		}
	}
	
	private static class HashCheckerException extends RuntimeException {
		private static final long serialVersionUID = 6487369255642692391L;
		private final AbandonOperationException wrapped;
		
		public HashCheckerException(String message) {
			super(new AbandonOperationException(message));
			wrapped = (AbandonOperationException) getCause();
		}

		public HashCheckerException(String message, Throwable cause) {
			super(new AbandonOperationException(message, cause));
			wrapped = (AbandonOperationException) getCause();
		}

		public AbandonOperationException getWrapped() {
			return wrapped;
		}
	}
}
