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
package org.structr.web.maintenance;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.schema.SchemaHelper;

/**
 *
 */
public class DeployDataCommand extends DeployCommand {

	private static final Logger logger                     = LoggerFactory.getLogger(DeployDataCommand.class.getName());

	private Map<String, List<Map<String, Object>>> relationshipMap;		// {relType: [{id:"", type:"", ...}], ..}
	private Set<String> alreadyExportedRelationships;

	private final static String DEPLOYMENT_DATA_IMPORT_STATUS   = "DEPLOYMENT_DATA_IMPORT_STATUS";
	private final static String DEPLOYMENT_DATA_EXPORT_STATUS   = "DEPLOYMENT_DATA_EXPORT_STATUS";

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("deployData", DeployDataCommand.class);
	}

	@Override
	public void doExport(final Map<String, Object> parameters) throws FrameworkException {

		final String path       = (String) parameters.get("target");
		final String types      = (String) parameters.get("types");

		if (StringUtils.isBlank(path)) {

			warn("Please provide target path for data deployment export.");
			throw new FrameworkException(422, "Please provide target path for data deployment export.");
		}

		if (StringUtils.isBlank(types)) {

			warn("Please provide a comma-separated list of type(s) to export. (e.g. 'ContentContainer,ContentItem')");
			throw new FrameworkException(422, "Please provide a comma-separated list of type(s) to export. (e.g. 'ContentContainer,ContentItem')");
		}

		final Path target  = Paths.get(path);

		try {

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("start",  startTime);
			broadcastData.put("target", target.toString());
			publishBeginMessage(DEPLOYMENT_DATA_EXPORT_STATUS, broadcastData);

			relationshipMap = new TreeMap();
			alreadyExportedRelationships = new HashSet();

			Files.createDirectories(target);

			final Path nodesDir    = Files.createDirectories(target.resolve("nodes"));
			final Path relsDir     = Files.createDirectories(target.resolve("relationships"));

			for (final String trimmedType : types.split(",")) {

				final String typeName = trimmedType.trim();

				final Class type = SchemaHelper.getEntityClassForRawType(typeName);

				if (type == null) {

					warn("Cannot export data for type '{}' - type unknown", typeName);

				} else {

					publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, "Exporting nodes for type " + typeName);

					final Path typeConf = nodesDir.resolve(typeName + ".json");

					exportDataForType(type, typeConf);
				}
			}

			for (final String relType : relationshipMap.keySet()) {

				publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, "Exporting relationships for type " + relType);

				final List<Map<String, Object>> relsForType = relationshipMap.get(relType);

				for (final Map<String, Object> rel : relsForType) {

					final Path relsConf = relsDir.resolve(relType + ".json");

					try (final Writer fos = new OutputStreamWriter(new FileOutputStream(relsConf.toFile()))) {

						getGson().toJson(relsForType, fos);

					} catch (IOException ioex) {
						logger.warn("", ioex);
					}
				}
			}

			final long endTime = System.currentTimeMillis();
			final DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

			customHeaders.put("end", new Date(endTime).toString());
			customHeaders.put("duration", duration);

			info("Data export of types '{}' to {} done. (Took {})", types, path, duration);

			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			publishEndMessage(DEPLOYMENT_DATA_EXPORT_STATUS, broadcastData);


		} catch (IOException ex) {
			logger.warn("", ex);
		}

	}

	@Override
	public void doImport(final Map<String, Object> parameters) throws FrameworkException {

		// backup previous value of change log setting and disable during deployment
		final boolean changeLogEnabled = Settings.ChangelogEnabled.getValue();
		Settings.ChangelogEnabled.setValue(false);

		try {

			missingPrincipals.clear();

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final String path         = (String) parameters.get("source");
			final SecurityContext ctx = SecurityContext.getSuperUserInstance();
			final App app             = StructrApp.getInstance(ctx);

			ctx.setDoTransactionNotifications(false);
			ctx.disableEnsureCardinality();
			ctx.disableModificationOfAccessTime();

			if (StringUtils.isBlank(path)) {

				warn("Please provide 'source' attribute for deployment source directory path.");
				throw new FrameworkException(422, "Please provide 'source' attribute for deployment source directory path.");
			}

			final Path source = Paths.get(path);
			if (!Files.exists(source)) {

				warn("Please provide 'source' attribute for deployment source directory path.");
				throw new FrameworkException(422, "Source path " + path + " does not exist.");
			}

			if (!Files.isDirectory(source)) {

				throw new FrameworkException(422, "Source path " + path + " is not a directory.");
			}

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("start",   startTime);
			broadcastData.put("source",  source.toString());
			publishBeginMessage(DEPLOYMENT_DATA_IMPORT_STATUS, broadcastData);


			final Path nodesDir = source.resolve("nodes");

			if (Files.exists(nodesDir)) {

				try {

					Files.list(nodesDir).forEach((Path p) -> {

						java.io.File f = p.toFile();

						if (f.isFile() && f.getName().endsWith(".json")) {

							final String typeName = StringUtils.substringBeforeLast(f.getName(), ".json");

							info("Importing nodes for type {}", typeName);
							publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, "Importing nodes for type " + typeName);

							try {

								importExtensibleNodeListData(typeName, readConfigList(p));

							} catch (FrameworkException ex) {

								logger.warn("Exception while importing nodes", ex);

							}
						}
					});

				} catch (IOException ex) {

					logger.warn("Exception while importing nodes", ex);
				}
			}

			final Path relsDir = source.resolve("relationships");

			if (Files.exists(relsDir)) {

				try {

					Files.list(relsDir).forEach((Path p) -> {

						java.io.File f = p.toFile();

						if (f.isFile() && f.getName().endsWith(".json")) {

							final String typeName = StringUtils.substringBeforeLast(f.getName(), ".json");

							info("Importing relationships for type {}", typeName);
							publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, "Importing relationships for type " + typeName);

							try {

								final Class type = SchemaHelper.getEntityClassForRawType(typeName);

								if (type == null) {
									throw new FrameworkException(422, "Type cannot be found: " + typeName);
								}

								importRelationshipListData(type, readConfigList(p));

							} catch (FrameworkException ex) {

								logger.warn("Exception while importing nodes", ex);

							}
						}
					});

				} catch (IOException ex) {

					logger.warn("Exception while importing nodes", ex);
				}
			}

			if (!missingPrincipals.isEmpty()) {

				final String title = "Missing Principal(s)";
				final String text = "The following user(s) and/or group(s) are missing for grants or node ownership during deployment.<br>"
						+ "Because of these missing grants/ownerships, the functionality is not identical to the export you just imported!<br><br>"
						+ String.join(", ",  missingPrincipals);

				info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tThe following user(s) and/or group(s) are missing for grants or node ownership during deployment.\n"
						+ "\tBecause of these missing grants/ownerships, the functionality is not identical to the export you just imported!\n\n"
						+ "\t" + String.join(", ",  missingPrincipals)
						+ "###############################################################################"
				);
				publishWarningMessage(title, text);
			}

			final long endTime = System.currentTimeMillis();
			final DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

			customHeaders.put("end", new Date(endTime).toString());
			customHeaders.put("duration", duration);

			info("Import from {} done. (Took {})", source.toString(), duration);

			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			publishEndMessage(DEPLOYMENT_DATA_IMPORT_STATUS, broadcastData);

		} finally {

			// restore saved value
			Settings.ChangelogEnabled.setValue(changeLogEnabled);
		}
	}

	private <T extends AbstractNode> void exportDataForType(final Class<T> nodeType, final Path targetConfFile) throws FrameworkException {

		final List<Map<String, Object>> nodes  = new LinkedList<>();
		final App app                          = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final T node : app.nodeQuery(nodeType).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				nodes.add(entry);

				putIfNotNull(entry, "id",                          node.getProperty(AbstractNode.id));
				putIfNotNull(entry, "name",                        node.getProperty(AbstractNode.name));
				putIfNotNull(entry, "type",                        node.getProperty(AbstractNode.type));
				putIfNotNull(entry, "hidden",                      node.getProperty(AbstractNode.hidden));
				putIfNotNull(entry, "visibleToPublicUsers",        node.getProperty(AbstractNode.visibleToPublicUsers));
				putIfNotNull(entry, "visibleToAuthenticatedUsers", node.getProperty(AbstractNode.visibleToAuthenticatedUsers));

				exportOwnershipAndSecurity(node, entry);
				exportCustomPropertiesForNode(node, entry);
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(targetConfFile.toFile()))) {

			getGson().toJson(nodes, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void exportCustomPropertiesForNode(final AbstractNode node, final Map<String, Object> map) throws FrameworkException {

		final SecurityContext context               = SecurityContext.getSuperUserInstance();
		final List<GraphObjectMap> customProperties = SchemaHelper.getSchemaTypeInfo(context, node.getType(), node.getClass(), "custom");

		for (final GraphObjectMap propertyInfo : customProperties) {

			final Map propInfo        = propertyInfo.toMap();
			final String propertyName = (String) propInfo.get("jsonName");

			if (propInfo.get("relatedType") == null) {
				map.put(propertyName, node.getProperty(StructrApp.key(node.getClass(), propertyName)));

			} else {

				if (Boolean.TRUE.equals(propInfo.get("isCollection"))) {

					final Iterable res = node.getProperty(StructrApp.key(node.getClass(), propertyName));
					if (res != null) {
						final Iterator<AbstractNode> it = res.iterator();

						while (it.hasNext()) {
							final AbstractNode relatedNode = it.next();
							final RelationshipInterface r = (RelationshipInterface) relatedNode.getPath(context);
							if (r != null) {
								exportRelationship(r);
							}
						}
					}

				} else {

					final AbstractNode relatedNode = node.getProperty(StructrApp.key(node.getClass(), propertyName));
					if (relatedNode != null) {
						final RelationshipInterface r = (RelationshipInterface) relatedNode.getPath(context);
						if (r != null) {
							exportRelationship(r);
						}
					}
				}
			}
		}
	}

	private void exportRelationship(final RelationshipInterface rel) throws FrameworkException {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final String relUuid = rel.getUuid();
			if (!alreadyExportedRelationships.contains(relUuid)) {

				final Map<String, Object> entry = new TreeMap<>();

				entry.put("sourceId", rel.getSourceNodeId());
				entry.put("targetId", rel.getTargetNodeId());

				exportCustomPropertiesForRel(rel, entry);

				addRelationshipToMap(rel.getClass().getSimpleName(), entry);

				alreadyExportedRelationships.add(relUuid);
			}

			tx.success();
		}
	}

	private void addRelationshipToMap (final String simpleRelType, Map<String, Object> relInfo) {

		List<Map<String, Object>> relsOfType = relationshipMap.get(simpleRelType);

		if (relsOfType == null) {
			relsOfType = new LinkedList();
			relationshipMap.put(simpleRelType, relsOfType);
		}

		relsOfType.add(relInfo);
	}

	private void exportCustomPropertiesForRel(final RelationshipInterface rel, final Map<String, Object> map) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();

		final List<GraphObjectMap> customProperties = SchemaHelper.getSchemaTypeInfo(context, rel.getType(), rel.getClass(), "custom");

		customProperties.stream().forEach((final GraphObjectMap propertyInfo) -> {

			final Map propInfo        = propertyInfo.toMap();
			final String propertyName = (String) propInfo.get("jsonName");

			map.put(propertyName, rel.getProperty(StructrApp.key(rel.getClass(), propertyName)));
		});
	}

	private <T extends NodeInterface> void importRelationshipListData(final Class type, final List<Map<String, Object>> data, final PropertyMap... additionalData) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final Map<String, Object> entry : data) {

				final String sourceId = (String) entry.get("sourceId");
				final String targetId = (String) entry.get("targetId");

				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, type, entry);

				// allow caller to insert additional data for better creation performance
				for (final PropertyMap add : additionalData) {
					map.putAll(add);
				}

				final NodeInterface sourceNode = app.getNodeById(sourceId);
				final NodeInterface targetNode = app.getNodeById(targetId);

				if (sourceNode == null) {

					logger.error("Unable to import relationship of type {}. Source node not found! {}", type.getSimpleName(), entry);

				} else if (targetNode == null) {

					logger.error("Unable to import relationship of type {}. Target node not found! {}", type.getSimpleName(), entry);

				} else {

					app.create(sourceNode, targetNode, type, map);

				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import {}, aborting with {}", type.getSimpleName(), fex.getMessage());
			fex.printStackTrace();

			throw fex;
		}
	}
}
