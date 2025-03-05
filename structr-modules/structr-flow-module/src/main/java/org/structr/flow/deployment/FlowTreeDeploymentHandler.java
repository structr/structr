/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.deployment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowContainerConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FlowTreeDeploymentHandler extends FlowAbstractDeploymentHandler implements FlowDeploymentInterface{

	private static final Logger logger                                                      = LoggerFactory.getLogger(FlowTreeDeploymentHandler.class.getName());
	private static final Gson gson                                                          = new GsonBuilder().setPrettyPrinting().create();
	private static final App app                                                            = StructrApp.getInstance();

	public final static String FLOW_DEPLOYMENT_TREE_BASE_FOLDER                             = "flows";
	private final static String FLOW_DEPLOYMENT_TREE_REL_FOLDER                             = "rels";
	private final static String FLOW_DEPLOYMENT_TREE_NODE_FOLDER                            = "nodes";
	private final static String FLOW_DEPLOYMENT_TREE_NODE_SCRIPTS_FOLDER                    = "scripts";
	private final static String FLOW_DEPLOYMENT_TREE_NODE_CHILDREN_FOLDER                   = "__children";
	private final static String FLOW_DEPLOYMENT_TREE_CONFIG_FOLDER                          = "config";

	private final static String FLOW_DEPLOYMENT_CONTAINER_FILE                              = "flow-container.json";
	private final static String FLOW_DEPLOYMENT_NODE_FILE                                   = "node.json";
	private final static String FLOW_DEPLOYMENT_REL_FILE                                    = "rel.json";
	private final static String FLOW_DEPLOYMENT_CONFIG_FILE                                 = "config.json";

	private static final String[] FLOW_BLACKLISTED_REL_TYPES                                = {"OWNS","SECURITY","CONTAINS_FLOW"};
	private static final String[] FLOW_SCRIPT_ATTRIBUTES									= {"query", "script", "result"};
	private static final String[] FLOW_IGNORE_WARNING_FOR_RELS								= {"FlowCallContainer"};


	@Override
	public void doExport(final Path target, final Gson gson) throws FrameworkException {

		try {

			final App app = StructrApp.getInstance();

			Path baseFolder = target.resolve(FLOW_DEPLOYMENT_TREE_BASE_FOLDER);

			// Delete any existing export data and reinitialize base folder
			FileUtils.deleteDirectory(baseFolder.toFile());
			baseFolder = Files.createDirectories(baseFolder);


			try (final Tx tx = app.tx()) {

				final Iterable<NodeInterface> flows = app.nodeQuery("FlowContainer").getResultStream();

				for (final NodeInterface flow : flows) {

					exportFlow(baseFolder, flow.as(FlowContainer.class));
				}

				tx.success();
			}

		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}

	}

	@Override
	public void doImport(final Path source, final Gson gson) throws FrameworkException {

		final Path flowFolder = source.resolve(FLOW_DEPLOYMENT_TREE_BASE_FOLDER);

		try (final Tx tx = app.tx()) {

			// Cleanup old flow data
			for (final String c : classesToExport) {

				for (final NodeInterface toDelete : app.nodeQuery(c).getAsList()) {

					app.delete(toDelete);
				}
			}

			for (final String c : relsToExport) {

				for (final RelationshipInterface toDelete : app.relationshipQuery(c).getAsList()) {

					app.delete(toDelete);
				}
			}


			for (final RelationshipInterface toDelete : app.relationshipQuery("FlowContainerConfigurationFlow").getAsList()) {

				app.delete(toDelete);
			}

			// Import new flow data
			visitFlowFolders(flowFolder, null);

			tx.success();
		}

	}


	// Private Methods
	private void visitFlowFolders(final Path flowFolder, final String packagePath) throws FrameworkException{

		// Start at flows base folder
		final File rootDir = new File(flowFolder.toAbsolutePath().toString());

		// Start import for each flow and their respective __children
		File[] children = rootDir.listFiles();

		if (children != null) {

			for (final File dir : children) {

				// Construct effective path based on current flow dir
				final String effectivePackagePath = packagePath != null ? packagePath + "." + dir.getName() : dir.getName();

				// Import flow from file
				if (dir.toPath().resolve(FLOW_DEPLOYMENT_CONTAINER_FILE).toFile().isFile()) {

					importFlow(dir.toPath(), effectivePackagePath);
				}

				File[] subDirs = dir.listFiles();
				if (subDirs != null) {

					for (final File subDir : subDirs) {

						if (subDir.getName().equals(FLOW_DEPLOYMENT_TREE_NODE_CHILDREN_FOLDER)) {

							// Import nested flow packages
							visitFlowFolders(subDir.toPath(), effectivePackagePath);
						}
					}
				}
			}
		}

	}

	private void importFlow(final Path flowRootDir, final String packagePath) throws FrameworkException {

		try {

			// 1. Create flow packages
			// 2. Create flow container
			final Map<String, Object> flowContainerData = readData(flowRootDir.resolve(FLOW_DEPLOYMENT_CONTAINER_FILE));
			final Traits flowContainerTraits            = Traits.of("FlowContainer");
			final NodeInterface flowContainer           = app.create("FlowContainer", convertMapToPropertyMap("FlowContainer", flowContainerData));

			// Set flow package implicitly
			flowContainer.setProperty(flowContainerTraits.key("effectiveName"), packagePath);

			// 3. Create flow nodes
			final File nodesDir = new File(flowRootDir.resolve(FLOW_DEPLOYMENT_TREE_NODE_FOLDER).toAbsolutePath().toString());


			try {
				for (final File nodeDir : Objects.requireNonNull(nodesDir.listFiles())) {

					// Import node with its base data
					final Map<String, Object> nodePropsData = readData(nodeDir.toPath().resolve(FLOW_DEPLOYMENT_NODE_FILE));
					final String type                       = nodePropsData.get("type").toString();
					final NodeInterface node                = app.create(type, convertMapToPropertyMap(type, nodePropsData));

					// Import node scripts
					final Path nodeScriptPath = nodeDir.toPath().resolve(FLOW_DEPLOYMENT_TREE_NODE_SCRIPTS_FOLDER);
					if (nodeScriptPath.toFile() != null && nodeScriptPath.toFile().isDirectory()) {

						final File nodeScriptsDir = new File(nodeScriptPath.toAbsolutePath().toString());

						for (final File nodeScript : Objects.requireNonNull(nodeScriptsDir.listFiles())) {

							// Read the script file and write it's content with it's name as property key
							final String attrName = nodeScript.getName();
							final String content = new String(Files.readAllBytes(nodeScript.toPath()));

							final PropertyKey propKey = node.getTraits().key(attrName);
							node.setProperty(propKey, content);
						}
					}
				}

			} catch (NullPointerException npe) {

				logger.warn("Traversed empty node directory during tree based flow import: " + nodesDir.toPath() + "\n This warning can be safely ignored, in case of an empty flow.");
			}

			// 4. Create flow container configuration
			final File configsDir = new File(flowRootDir.resolve(FLOW_DEPLOYMENT_TREE_CONFIG_FOLDER).toAbsolutePath().toString());

			try {

				for (final File configDir : Objects.requireNonNull(configsDir.listFiles())) {

					final Map<String, Object> configPropsData = readData(configDir.toPath().resolve(FLOW_DEPLOYMENT_CONFIG_FILE));

					app.create("FlowContainerConfiguration", convertMapToPropertyMap("FlowContainerConfiguration", configPropsData));
				}

			} catch (NullPointerException npe) {
				logger.warn("Traversed empty  config directory during tree based flow import: " + nodesDir.toPath());
			}

			// 5. Create rels

			final File relsDir = new File(flowRootDir.resolve(FLOW_DEPLOYMENT_TREE_REL_FOLDER).toAbsolutePath().toString());

			try {
				for (final File relDir : Objects.requireNonNull(relsDir.listFiles())) {

					// Import rels
					final Map<String, Object> relPropsData = readData(relDir.toPath().resolve(FLOW_DEPLOYMENT_REL_FILE));
					final String relType                   = relPropsData.get("type").toString();

					final NodeInterface fromNode = app.getNodeById(relPropsData.get("sourceId").toString());
					final NodeInterface toNode   = app.getNodeById(relPropsData.get("targetId").toString());

					if (fromNode != null && toNode != null) {

						final RelationshipInterface rel = app.create(fromNode, toNode, relType);

						rel.unlockSystemPropertiesOnce();
						rel.setProperty(Traits.of("RelationshipInterface").key("id"), relPropsData.get("id").toString());

					} else if (!Arrays.asList(FLOW_IGNORE_WARNING_FOR_RELS).contains(relType)) {

						logger.warn("Could not import rel data for: " + gson.toJson(relPropsData));
					}
				}

			} catch (NullPointerException npe) {

				logger.warn("Traversed empty rels directory during tree based flow import: " + nodesDir.toPath());
			}

		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}

	}

	private void exportFlow(final Path target, final FlowContainer flow) throws FrameworkException {

		try {

			final String effectiveName                  = flow.getEffectiveName();
			final String effectiveFlowPath              = effectiveName.contains(".") ? String.join("/"+ FLOW_DEPLOYMENT_TREE_NODE_CHILDREN_FOLDER + "/",effectiveName.split("\\.")) : effectiveName;

			final Path flowFolder                       = Files.createDirectories(target.resolve(effectiveFlowPath));
			final Path nodePath                         = Files.createDirectories(flowFolder.resolve(FLOW_DEPLOYMENT_TREE_NODE_FOLDER));
			final Path relPath                          = Files.createDirectories(flowFolder.resolve(FLOW_DEPLOYMENT_TREE_REL_FOLDER));
			final Path configPath                       = Files.createDirectories(flowFolder.resolve(FLOW_DEPLOYMENT_TREE_CONFIG_FOLDER));

			// 1. Export flow container
			writeData(flowFolder.resolve(FLOW_DEPLOYMENT_CONTAINER_FILE), gson.toJson(flow.exportData()));

			for (final RelationshipInterface rel : flow.getRelationships()) {
				exportRelationship(relPath, rel);
			}

			// 2. Export all nodes contained within the flow
			final Iterable<FlowBaseNode> nodes = flow.getFlowNodes();

			for (final FlowBaseNode node : nodes) {

				final Map<String, Object> exportData   = node.exportData();
				final Map<String, String> scriptData   = new HashMap<>();

				// Remove scripts from exportData and export them seperately
				for (final String key : FLOW_SCRIPT_ATTRIBUTES) {
					if (exportData.containsKey(key)) {
						if (exportData.get(key) != null) {
							scriptData.put(key, exportData.get(key).toString());
						}
						exportData.remove(key);
					}
				}

				// Write node base data
				writeData(Files.createDirectories(nodePath.resolve(node.getUuid())).resolve(FLOW_DEPLOYMENT_NODE_FILE), gson.toJson(exportData));

				// Write node scripts
				for (final String key : scriptData.keySet()) {

					writeData(Files.createDirectories(nodePath.resolve(node.getUuid()).resolve(FLOW_DEPLOYMENT_TREE_NODE_SCRIPTS_FOLDER)).resolve(key), scriptData.get(key));
				}

				// Write rels for node
				for (final RelationshipInterface rel : node.getRelationships()) {
					exportRelationship(relPath, rel);
				}
			}

			// 3. Export flow container config
			final Iterable<FlowContainerConfiguration> configs = flow.getFlowConfigurations();
			for (final FlowContainerConfiguration conf : configs) {

				writeData(Files.createDirectories(configPath.resolve(conf.getUuid())).resolve(FLOW_DEPLOYMENT_CONFIG_FILE), gson.toJson(conf.exportData()));
			}


		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}
	}


	private void exportRelationship(final Path target, final RelationshipInterface rel) throws FrameworkException {

		try {

			if (!Arrays.asList(FLOW_BLACKLISTED_REL_TYPES).contains(rel.getRelType().name())) {

				final Path relPath = Files.createDirectories(target.resolve(rel.getUuid()));

				Map<String, String> attrs = new TreeMap<>();

				attrs.put("id",       rel.getUuid());
				attrs.put("type",     rel.getType());
				attrs.put("relType",  rel.getRelType().name());
				attrs.put("sourceId", rel.getSourceNodeId());
				attrs.put("targetId", rel.getTargetNodeId());

				writeData(relPath.resolve(FLOW_DEPLOYMENT_REL_FILE), gson.toJson(attrs));
			}

		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}


	}

	private PropertyMap convertMapToPropertyMap(final String type, final Map<String,Object> map) {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(type);

		for (final String key : map.keySet()) {

			props.put(traits.key(key), map.get(key));
		}

		return props;
	}

	private Map<String,Object> readData(final Path target) throws FrameworkException {

		Map<String,Object> result = new HashMap<>();

		try (final Reader fis = new InputStreamReader(new FileInputStream(target.toFile()))) {

			result = gson.fromJson(fis, Map.class);

		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}

		return result;
	}

	private void writeData(final Path target, final String data) {

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			fos.write(data);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}
}
