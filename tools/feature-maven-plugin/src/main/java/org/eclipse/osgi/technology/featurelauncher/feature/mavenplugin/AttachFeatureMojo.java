/*********************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * Attaches a feature.json file as a Maven artifact for deployment.
 * <p>
 * This goal attaches the feature file to the project so it will be deployed
 * along with other artifacts when running `mvn deploy`.
 * <p>
 * The feature will be deployed with:
 * <ul>
 *   <li>Type: json</li>
 *   <li>Classifier: feature (default, configurable)</li>
 * </ul>
 * <p>
 * Usage in pom.xml:
 * <pre>
 * &lt;plugin&gt;
 *     &lt;groupId&gt;org.eclipse.osgi-technology.featurelauncher&lt;/groupId&gt;
 *     &lt;artifactId&gt;feature-maven-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *         &lt;execution&gt;
 *             &lt;goals&gt;
 *                 &lt;goal&gt;attach-feature&lt;/goal&gt;
 *             &lt;/goals&gt;
 *         &lt;/execution&gt;
 *     &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 * <p>
 * After running `mvn deploy`, the feature can be referenced as:
 * <pre>
 * groupId:artifactId:version:json:feature
 * </pre>
 */
@Mojo(name = "attach-feature", defaultPhase = LifecyclePhase.PACKAGE)
public class AttachFeatureMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The feature file to attach.
     */
    @Parameter(property = "feature.file",
               defaultValue = "${project.build.directory}/feature.json")
    private File featureFile;

    /**
     * The classifier to use for the attached artifact.
     * Default is "feature".
     */
    @Parameter(property = "feature.classifier", defaultValue = "feature")
    private String classifier;

    /**
     * The type/extension for the attached artifact.
     * Default is "json".
     */
    @Parameter(property = "feature.type", defaultValue = "json")
    private String type;

    /**
     * Skip this goal.
     */
    @Parameter(property = "feature.attach.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping attach-feature");
            return;
        }

        if (!featureFile.exists()) {
            throw new MojoExecutionException(
                "Feature file not found: " + featureFile.getAbsolutePath() +
                "\nRun 'osgifeature:create-feature' first to generate the feature file.");
        }

        getLog().info("Attaching feature file: " + featureFile.getName());
        getLog().info("  Classifier: " + classifier);
        getLog().info("  Type: " + type);

        projectHelper.attachArtifact(project, type, classifier, featureFile);

        String coords = String.format("%s:%s:%s:%s:%s",
            project.getGroupId(),
            project.getArtifactId(),
            project.getVersion(),
            type,
            classifier);

        getLog().info("Feature attached. Maven coordinates: " + coords);
        getLog().info("Deploy with: mvn deploy");
    }
}
