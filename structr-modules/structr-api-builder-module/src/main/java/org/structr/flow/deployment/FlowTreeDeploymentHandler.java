/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowContainerConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class FlowTreeDeploymentHandler implements FlowDeploymentInterface{

	private static final Logger logger                                                      = LoggerFactory.getLogger(FlowTreeDeploymentHandler.class.getName());
	private static final Gson gson                                                          = new Gson();

	public final static String FLOW_DEPLOYMENT_TREE_BASE_FOLDER                            = "flows";
	private final static String FLOW_DEPLOYMENT_TREE_REL_FOLDER                             = "rels";
	private final static String FLOW_DEPLOYMENT_TREE_NODE_FOLDER                            = "nodes";
	private final static String FLOW_DEPLOYMENT_TREE_NODE_SCRIPTS_FOLDER                    = "scripts";
	private final static String FLOW_DEPLOYMENT_TREE_NODE_CHILDREN_FOLDER                   = "__children";
	private final static String FLOW_DEPLOYMENT_TREE_CONFIG_FOLDER                          = "config";

	private final static String FLOW_DEPLOYMENT_CONTAINER_FILE                              = "flow-container.json";
	private final static String FLOW_DEPLOYMENT_NODE_FILE                                   = "node.json";
	private final static String FLOW_DEPLOYMENT_REL_FILE                                    = "rel.json";
	private final static String FLOW_DEPLOYMENT_CONFIG_FILE                                 = "config.json";

	private static final String[] FLOW_SCRIPT_ATTRIBUTES									= {"query", "script", "result"};


	@Override
	public void doExport(final Path target, final Gson gson) throws FrameworkException {

		try {

			final App app = StructrApp.getInstance();

			final Path baseFolder = Files.createDirectories(target.resolve(FLOW_DEPLOYMENT_TREE_BASE_FOLDER));

			try (final Tx tx = app.tx()) {

				final Iterable<FlowContainer> flows = app.nodeQuery(FlowContainer.class);

				for (final FlowContainer flow : flows) {

					exportFlow(baseFolder, flow);
				}

			}

		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}

	}

	@Override
	public void doImport(final Path source, final Gson gson) throws FrameworkException {

		final App app = StructrApp.getInstance();

		final Path flowFolder = source.resolve(FLOW_DEPLOYMENT_TREE_BASE_FOLDER);

		try (final Tx tx = app.tx()) {

			visitFlowFolders(flowFolder, null);

			tx.success();
		}

	}


	// Private Methods
	private void visitFlowFolders(final Path flowFolder, final String packagePath) throws FrameworkException{

		// Start at flows base folder
		final File rootDir = new File(flowFolder.toAbsolutePath().toString());
		File[] children = rootDir.listFiles();

		// Start import for each flow and their respective __children
		if (children != null) {

			for (final File dir : children) {

				// Construct effective path based on current flow dir
				final String effectivePackagePath = packagePath != null ? packagePath + "." + dir.getName() : dir.getName();

				importFlow(dir.toPath(), packagePath);

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

		// 1. Create flow packages

		// 2. Create flow container

		// 3. Create flow nodes

		// 4. Create flow container configuration

		// 5. Create rels


	}

	private void exportFlow(final Path target, final FlowContainer flow) throws FrameworkException {

		try {

			final String effectiveName                  = flow.getProperty(FlowContainer.effectiveName).toString();
			final String effectiveFlowPath              = effectiveName.contains(".") ? String.join("/"+ FLOW_DEPLOYMENT_TREE_NODE_CHILDREN_FOLDER + "/",effectiveName.split("\\.")) : effectiveName;

			final Path flowFolder                       = Files.createDirectories(target.resolve(effectiveFlowPath));
			final Path nodePath                         = Files.createDirectories(flowFolder.resolve(FLOW_DEPLOYMENT_TREE_NODE_FOLDER));
			final Path relPath                          = Files.createDirectories(flowFolder.resolve(FLOW_DEPLOYMENT_TREE_REL_FOLDER));
			final Path configPath                       = Files.createDirectories(flowFolder.resolve(FLOW_DEPLOYMENT_TREE_CONFIG_FOLDER));

			// 1. Export flow container
			writeData(flowFolder.resolve(FLOW_DEPLOYMENT_CONTAINER_FILE), gson.toJson(flow.exportData()));

			for (final AbstractRelationship rel : flow.getRelationships()) {
				exportRelationship(relPath, rel);
			}

			// 2. Export all nodes contained within the flow
			final Iterable<FlowBaseNode> nodes = flow.getProperty(FlowContainer.flowNodes);

			for (final FlowBaseNode node : nodes) {

				final Map<String, Object> exportData   = node.exportData();
				final Map<String, String> scriptData   = new HashMap<>();

				// Remove scripts from exportData and export them seperately
				for (final String key : FLOW_SCRIPT_ATTRIBUTES) {
					if (exportData.containsKey(key)) {
						scriptData.put(key, exportData.get(key).toString());
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
				for (final AbstractRelationship rel : node.getRelationships()) {
					exportRelationship(relPath, rel);
				}
			}

			// 3. Export flow container config
			final Iterable<FlowContainerConfiguration> configs = flow.getProperty(FlowContainer.flowConfigurations);
			for (final FlowContainerConfiguration conf : configs) {
				writeData(Files.createDirectories(configPath.resolve(conf.getUuid())).resolve(FLOW_DEPLOYMENT_CONFIG_FILE), gson.toJson(conf.exportData()));
			}


		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}
	}


	private void exportRelationship(final Path target, final AbstractRelationship rel) throws FrameworkException {

		try {

			final Path relPath = Files.createDirectories(target.resolve(rel.getUuid()));

			Map<String, String> attrs = new TreeMap<>();
			attrs.put("type", rel.getClass().getSimpleName());
			attrs.put("relType", ((RelationshipInterface) rel).getRelType().name());
			attrs.put("sourceId", ((RelationshipInterface) rel).getSourceNodeId());
			attrs.put("targetId", ((RelationshipInterface) rel).getTargetNodeId());

			writeData(relPath.resolve(FLOW_DEPLOYMENT_REL_FILE), gson.toJson(attrs));

		} catch (IOException ex) {

			throw new FrameworkException(500, ex.getMessage());
		}


	}

	private void writeData(final Path target, final String data) {
		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			fos.write(data);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}
}
