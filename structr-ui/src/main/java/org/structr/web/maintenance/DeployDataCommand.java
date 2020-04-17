/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import com.google.gson.Gson;
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
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.ResultStream;
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
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.User;

public class DeployDataCommand extends DeployCommand {

	private static final Logger logger                     = LoggerFactory.getLogger(DeployDataCommand.class.getName());

	private Map<String, List<Map<String, Object>>> relationshipMap;
	private Set<String> alreadyExportedRelationships;

	private HashSet<Class> exportTypes;
	private HashSet<Class> missingTypesForExport;
	private HashSet<String> missingTypeNamesForExport;

	private Set<String> missingTypesForImport;
	private Map<String, Integer> failedRelationshipImports;

	private final static String DEPLOYMENT_DATA_IMPORT_NODE_DIRECTORY   = "nodes";
	private final static String DEPLOYMENT_DATA_IMPORT_RELS_DIRECTORY   = "relationships";

	private final static String DEPLOYMENT_DATA_IMPORT_STATUS   = "DEPLOYMENT_DATA_IMPORT_STATUS";
	private final static String DEPLOYMENT_DATA_EXPORT_STATUS   = "DEPLOYMENT_DATA_EXPORT_STATUS";

	private boolean doInnerCallbacks  = false;
	private boolean doCascadingDelete = false;


	static {

		MaintenanceParameterResource.registerMaintenanceCommand("deployData", DeployDataCommand.class);
	}

	@Override
	public void doExport(final Map<String, Object> parameters) throws FrameworkException {

		final String path       = (String) parameters.get("target");
		final String types      = (String) parameters.get("types");

		if (StringUtils.isBlank(path)) {

			logger.warn("Please provide target path for data deployment export.");
			throw new FrameworkException(422, "Please provide target path for data deployment export.");
		}

		if (StringUtils.isBlank(types)) {

			logger.warn("Please provide a comma-separated list of type(s) to export. (e.g. 'ContentContainer,ContentItem')");
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

			exportTypes                  = new HashSet();
			missingTypeNamesForExport    = new HashSet();
			missingTypesForExport        = new HashSet();
			relationshipMap              = new TreeMap();
			alreadyExportedRelationships = new HashSet();

			Files.createDirectories(target);

			final Path nodesDir = Files.createDirectories(target.resolve(DEPLOYMENT_DATA_IMPORT_NODE_DIRECTORY));
			final Path relsDir  = Files.createDirectories(target.resolve(DEPLOYMENT_DATA_IMPORT_RELS_DIRECTORY));

			// first determine all requested types
			for (final String trimmedType : types.split(",")) {

				final String typeName = trimmedType.trim();

				final Class type = SchemaHelper.getEntityClassForRawType(typeName);

				if (type == null) {

					logger.warn("Cannot export data for type '{}' - type unknown", typeName);
					publishWarningMessage("Type not found", "Cannot export data for type '" + typeName + "' - type unknown");

				} else {

					exportTypes.add(type);
				}
			}

			removeInheritingTypesFromSet(exportTypes);
			removeFileTypesFromSet(exportTypes);

			final SecurityContext context = getRecommendedSecurityContext();

			for (final Class type : exportTypes) {

				if (User.class.isAssignableFrom(type)) {

					logger.warn("User type in export set! Type '{}' is a User type.\n\tIf the user who is running the import is present in the import, this can lead to problems!", type.getSimpleName());
					publishWarningMessage("User type in export set", "Type '" + type.getSimpleName() + "' is a User type.<br>If the user who is running the import is present in the import, this can lead to problems!");
				}

				publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, "Exporting nodes for type " + type.getSimpleName());

				final Path typeConf = nodesDir.resolve(type.getSimpleName() + ".json");

				exportDataForType(context, type, typeConf);
			}

			for (final String relType : relationshipMap.keySet()) {

				publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, "Exporting relationships for type " + relType);

				final List<Map<String, Object>> relsForType = relationshipMap.get(relType);

				final Path relsConf = relsDir.resolve(relType + ".json");

				writeJsonToFile(relsConf, relsForType);
			}

			if (!missingTypeNamesForExport.isEmpty()) {

				final String title = "Possibly missing type(s) in export";
				final String text = "Relationships to/from the following type(s) were exported during <b>data deployment</b> but the type(s) (or supertype(s)) were not in the export set.<br>"
						+ "The affected relationships will probably not be imported during a data deployment import.<br><br>"
						+ String.join(", ",  missingTypeNamesForExport)
						+ "<br><br>You might want to include those types in the export set or delete the resulting file(s) in the data export.";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tRelationships to/from the following type(s) were exported during data deployment but the type(s) (or supertype(s)) were not in the export set.\n"
						+ "\tThe affected relationships will probably not be imported during a data deployment import.\n\n"
						+ "\t" + String.join(", ",  missingTypeNamesForExport)
						+ "\n\n\tYou might want to include those types in the export set or delete the resulting file(s) in the data export.\n"
						+ "###############################################################################"
				);

				publishWarningMessage(title, text);
			}

			final long endTime = System.currentTimeMillis();
			final DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

			customHeaders.put("end", new Date(endTime).toString());
			customHeaders.put("duration", duration);

			logger.info("Data export of types '{}' to {} done. (Took {})", types, path, duration);

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

			final String path = (String) parameters.get("source");

			if (StringUtils.isBlank(path)) {

				logger.warn("Please provide 'source' attribute for deployment source directory path.");
				throw new FrameworkException(422, "Please provide 'source' attribute for deployment source directory path.");
			}

			final Path source = Paths.get(path);
			if (!Files.exists(source)) {

				logger.warn("Please provide 'source' attribute for deployment source directory path.");
				throw new FrameworkException(422, "Source path '" + path + "' does not exist.");
			}

			if (!Files.isDirectory(source)) {

				throw new FrameworkException(422, "Source path '" + path + "' is not a directory.");
			}

			doInnerCallbacks  = parameters.get("doInnerCallbacks") == null  ? false : "true".equals(parameters.get("doInnerCallbacks").toString());
			doCascadingDelete = parameters.get("doCascadingDelete") == null ? false : "true".equals(parameters.get("doCascadingDelete").toString());

			doImportFromDirectory(source);

		} catch (Throwable t) {

			publishWarningMessage("Fatal Error", "Something went wrong - the deployment import has stopped. Please see the log for more information");

			throw t;

		} finally {

			// restore saved value
			Settings.ChangelogEnabled.setValue(changeLogEnabled);
		}
	}

	public void doImportFromDirectory(final Path source) {

		missingTypesForImport = new HashSet();
		failedRelationshipImports = new TreeMap();

		final long startTime = System.currentTimeMillis();
		customHeaders.put("start", new Date(startTime).toString());

		final Map<String, Object> broadcastData = new HashMap();
		broadcastData.put("start",   startTime);
		broadcastData.put("source",  source.toString());
		publishBeginMessage(DEPLOYMENT_DATA_IMPORT_STATUS, broadcastData);

		final SecurityContext context = getRecommendedSecurityContext();

		if (Boolean.TRUE.equals(doInnerCallbacks)) {

			logger.info("You provided the 'doInnerCallbacks' parameter - if you encounter problems caused by onCreate/onSave methods or function properties, you might want to disable this.");
			context.enableInnerCallbacks();
		}

		if (Boolean.TRUE.equals(doCascadingDelete)) {

			logger.info("You provided the 'doCascadingDelete' parameter - if you encounter problems caused by nodes being deleted, you might want to disable this.");
			context.setDoCascadingDelete(true);
		}

		// apply pre-deploy.conf
		applyConfigurationFile(context, source.resolve("pre-data-deploy.conf"), DEPLOYMENT_DATA_IMPORT_STATUS);

		// do the actual import
		final Path nodesDir = source.resolve(DEPLOYMENT_DATA_IMPORT_NODE_DIRECTORY);

		if (Files.exists(nodesDir)) {

			try {

				Files.list(nodesDir).forEach((Path p) -> {

					java.io.File f = p.toFile();

					if (f.isFile() && f.getName().endsWith(".json")) {

						final String typeName = StringUtils.substringBeforeLast(f.getName(), ".json");

						logger.info("Importing nodes for type {}", typeName);
						publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, "Importing nodes for type " + typeName);

						importExtensibleNodeListData(context, typeName, readConfigList(p));
					}
				});

			} catch (IOException ex) {

				logger.warn("Exception while importing nodes", ex);
			}
		}

		final Path relsDir = source.resolve(DEPLOYMENT_DATA_IMPORT_RELS_DIRECTORY);

		if (Files.exists(relsDir)) {

			try {

				Files.list(relsDir).forEach((Path p) -> {

					java.io.File f = p.toFile();

					if (f.isFile() && f.getName().endsWith(".json")) {

						final String typeName = StringUtils.substringBeforeLast(f.getName(), ".json");

						final Class type = SchemaHelper.getEntityClassForRawType(typeName);

						if (type == null) {

							logger.warn("Not importing data. Relationship type cannot be found: {}!", typeName);
							publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, "Type can not be found! NOT Importing relationships for type " + typeName);

						} else {

							logger.info("Importing relationships for type {}", typeName);
							publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, "Importing relationships for type " + typeName);

							importRelationshipListData(context, type, readConfigList(p));
						}
					}
				});

			} catch (IOException ex) {

				logger.warn("Exception while importing nodes", ex);
			}
		}

		// apply post-deploy.conf
		applyConfigurationFile(context, source.resolve("post-data-deploy.conf"), DEPLOYMENT_DATA_IMPORT_STATUS);

		if (!missingPrincipals.isEmpty()) {

			final String title = "Missing Principal(s)";
			final String text = "The following user(s) and/or group(s) are missing for grants or node ownership during <b>data deployment</b>.<br>"
					+ "Because of these missing grants/ownerships, <b>the functionality is not identical to the export you just imported</b>!<br><br>"
					+ String.join(", ",  missingPrincipals)
					+ "<br><br>Consider adding these principals to your <a href=\"https://support.structr.com/article/428#pre-deployconf-javascript\">pre-data-deploy.conf</a> and re-importing.";

			logger.info("\n###############################################################################\n"
					+ "\tWarning: " + title + "!\n"
					+ "\tThe following user(s) and/or group(s) are missing for grants or node ownership during deployment.\n"
					+ "\tBecause of these missing grants/ownerships, the functionality is not identical to the export you just imported!\n\n"
					+ "\t" + String.join(", ",  missingPrincipals)
					+ "\n\n\tConsider adding these principals to your 'pre-data-deploy.conf' (see https://support.structr.com/article/428#pre-deployconf-javascript) and re-importing.\n"
					+ "###############################################################################"
			);

			publishWarningMessage(title, text);
		}

		if (!missingTypesForImport.isEmpty()) {

			final String title = "Missing Type(s)";
			final String text = "The following type(s) do not exist in the current schema but are present in the data deployment.<br>"
					+ "Make sure you are importing the data to the correct schema! (See the log for more details)<br><br>"
					+ String.join(", ", missingTypesForImport);

			publishWarningMessage(title, text);
		}

		if (!failedRelationshipImports.isEmpty()) {

			final String infos = failedRelationshipImports.keySet().stream().reduce("", (tmp, typeName) -> { return tmp + typeName + ": " + failedRelationshipImports.get(typeName) + "<br>"; });

			final String title = "Failed Relationship(s) for Type(s)";
			final String text = "The following list shows the number of failed relationship imports per type.<br>"
					+ "Make sure you are importing the data to the correct schema and that both ends of the relationship(s) are in the database/export! (See the log for more details)<br><br>"
					+ infos;

			publishWarningMessage(title, text);
		}


		final long endTime = System.currentTimeMillis();
		final DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

		customHeaders.put("end", new Date(endTime).toString());
		customHeaders.put("duration", duration);

		logger.info("Import from {} done. (Took {})", source.toString(), duration);

		broadcastData.put("end", endTime);
		broadcastData.put("duration", duration);
		publishEndMessage(DEPLOYMENT_DATA_IMPORT_STATUS, broadcastData);

	}

	protected SecurityContext getRecommendedSecurityContext () {

		final SecurityContext context = SecurityContext.getSuperUserInstance();

		context.setDoTransactionNotifications(false);
		context.disablePreventDuplicateRelationships();
		context.disableModificationOfAccessTime();
		context.disableInnerCallbacks();
		context.setDoCascadingDelete(false);

		return context;

	}

	private void removeInheritingTypesFromSet(final HashSet<Class> types) {

		final Map<String, String> removedTypes = new HashMap();

		final HashSet<Class> clonedTypes = new HashSet();
		clonedTypes.addAll(types);

		for (final Class childType : clonedTypes) {

			for (final Class parentType : clonedTypes) {

				if (parentType != childType && parentType.isAssignableFrom(childType)) {

					removedTypes.put(childType.getSimpleName(), parentType.getSimpleName());

					types.remove(childType);
				}
			}
		}

		if (!removedTypes.isEmpty()) {

			final List<String> messages = new LinkedList();

			for (String childType : removedTypes.keySet()) {

				messages.add(childType + " inherits from " + removedTypes.get(childType));
			}

			logger.warn("The following types were removed from the export set because a parent type is also being exported:\n" + String.join("\n", messages));
			publishWarningMessage("Type(s) removed from export set", "The following types were removed from the export set because a parent type is also being exported:<br>" + String.join("<br>", messages));
		}
	}

	private void removeFileTypesFromSet(final HashSet<Class> types) {

		final HashSet<Class> clonedTypes = new HashSet();
		clonedTypes.addAll(types);

		for (final Class type : clonedTypes) {

			if (AbstractFile.class.isAssignableFrom(type)) {

				logger.warn("Removing type '{}' because it inherits from 'File' which can not be exported via data deployment.", type.getSimpleName());
				publishWarningMessage("Type removed from export set", "Type '" + type.getSimpleName() + "' is a File type.<br> Currently it does not work exporting file types in data deployment.");

				types.remove(type);
			}
		}
	}

	private <T extends AbstractNode> void exportDataForType(final SecurityContext context, final Class<T> nodeType, final Path targetConfFile) throws FrameworkException {

		final App app = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			try (final Writer fos = new OutputStreamWriter(new FileOutputStream(targetConfFile.toFile()))) {

				final Gson gson = getGson();
				boolean dataWritten = false;

				fos.write("[");

				try (final ResultStream<T> resultStream = app.nodeQuery(nodeType).getResultStream()) {

					for (final T node : resultStream) {

						final Map<String, Object> entry = new TreeMap<>();

						final PropertyContainer pc = node.getPropertyContainer();

						for (final String key : pc.getPropertyKeys()) {
							putData(entry, key, pc.getProperty(key));
						}

						exportOwnershipAndSecurity(node, entry);
						exportRelationshipsForNode(context, node);

						if (dataWritten) {
							fos.write(",");
						}
						fos.write("\n");

						gson.toJson(entry, fos);

						dataWritten = true;
					}
				}

				if (dataWritten) {
					fos.write("\n");
				}

				fos.write("]");

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}

			tx.success();
		}
	}

	private void exportRelationshipsForNode(final SecurityContext context, final AbstractNode node) throws FrameworkException {

		final List<GraphObjectMap> customProperties = SchemaHelper.getSchemaTypeInfo(context, node.getType(), node.getClass(), "custom");

		for (final GraphObjectMap propertyInfo : customProperties) {

			final Map propInfo        = propertyInfo.toMap();
			final String propertyName = (String) propInfo.get("jsonName");

			if (propInfo.get("relatedType") != null && !propInfo.get("className").equals("org.structr.core.property.EntityNotionProperty") && !propInfo.get("className").equals("org.structr.core.property.CollectionNotionProperty")) {

				if (Boolean.TRUE.equals(propInfo.get("isCollection"))) {

					final Iterable res = node.getProperty(StructrApp.key(node.getClass(), propertyName));
					if (res != null) {
						final Iterator<AbstractNode> it = res.iterator();

						while (it.hasNext()) {
							final AbstractNode relatedNode = it.next();
							final RelationshipInterface r = (RelationshipInterface) relatedNode.getPath(context);
							if (r != null) {
								exportRelationship(context, r);
							}
						}
					}

				} else {

					final AbstractNode relatedNode = node.getProperty(StructrApp.key(node.getClass(), propertyName));
					if (relatedNode != null) {
						final RelationshipInterface r = (RelationshipInterface) relatedNode.getPath(context);
						if (r != null) {
							exportRelationship(context, r);
						}
					}
				}
			}
		}
	}

	private boolean isTypeInExportedTypes(final Class type) {

		for (final Class exportedType : exportTypes) {

			if (exportedType.isAssignableFrom(type)) {

				return true;
			}
		}

		return false;
	}

	private void exportRelationship(final SecurityContext context, final RelationshipInterface rel) throws FrameworkException {

		final App app = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			final String relUuid = rel.getUuid();
			if (!alreadyExportedRelationships.contains(relUuid)) {

				final Map<String, Object> entry = new TreeMap<>();

				final Class sourceNodeClass = rel.getSourceNode().getClass();
				final Class targetNodeClass = rel.getTargetNode().getClass();

				if (!missingTypesForExport.contains(sourceNodeClass) && !isTypeInExportedTypes(sourceNodeClass)) {

					missingTypeNamesForExport.add(sourceNodeClass.getSimpleName());
				}

				if (!missingTypesForExport.contains(targetNodeClass) && !isTypeInExportedTypes(targetNodeClass)) {

					missingTypeNamesForExport.add(targetNodeClass.getSimpleName());
				}

				entry.put("sourceId", rel.getSourceNodeId());
				entry.put("targetId", rel.getTargetNodeId());

				final PropertyContainer pc = rel.getPropertyContainer();

				for (final String key : pc.getPropertyKeys()) {
					putData(entry, key, pc.getProperty(key));
				}

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

	private <T extends NodeInterface> void importRelationshipListData(final SecurityContext context, final Class type, final List<Map<String, Object>> data) {

		final App app = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final Map<String, Object> entry : data) {

				final String sourceId = (String) entry.get("sourceId");
				final String targetId = (String) entry.get("targetId");

				final NodeInterface sourceNode = app.getNodeById(sourceId);
				final NodeInterface targetNode = app.getNodeById(targetId);

				if (sourceNode == null) {

					logger.error("Unable to import relationship of type {}. Source node not found! {}", type.getSimpleName(), sourceId);

					if (!failedRelationshipImports.containsKey(type.getSimpleName())) {
						failedRelationshipImports.put(type.getSimpleName(), 0);
					}
					failedRelationshipImports.put(type.getSimpleName(), failedRelationshipImports.get(type.getSimpleName()) + 1);

				} else if (targetNode == null) {

					logger.error("Unable to import relationship of type {}. Target node not found! {}", type.getSimpleName(), targetId);

					if (!failedRelationshipImports.containsKey(type.getSimpleName())) {
						failedRelationshipImports.put(type.getSimpleName(), 0);
					}
					failedRelationshipImports.put(type.getSimpleName(), failedRelationshipImports.get(type.getSimpleName()) + 1);

				} else {

					final RelationshipInterface r = app.create(sourceNode, targetNode, type);

					correctNumberFormats(context, entry, type);

					final PropertyContainer pc = r.getPropertyContainer();
					pc.setProperties(entry);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import relationships for type {}. Cause: {}", type.getSimpleName(), fex.getMessage());
			fex.printStackTrace();
		}
	}

	private <T extends NodeInterface> void importExtensibleNodeListData(final SecurityContext context, final String defaultTypeName, final List<Map<String, Object>> data) {

		final Class defaultType = SchemaHelper.getEntityClassForRawType(defaultTypeName);

		if (defaultType == null) {

			logger.warn("Not importing data. Node type cannot be found: {}!", defaultTypeName);

			missingTypesForImport.add(defaultTypeName);

		} else {

			final App app = StructrApp.getInstance(context);

			try (final Tx tx = app.tx()) {

				tx.disableChangelog();

				for (final Map<String, Object> entry : data) {

					final String id = (String)entry.get("id");

					if (id != null) {

						final NodeInterface existingNode = app.getNodeById(id);

						if (existingNode != null) {

							app.delete(existingNode);
						}
					}

					checkOwnerAndSecurity(entry);

					final String typeName = (String) entry.get("type");
					final Class type      = ((typeName == null || defaultTypeName.equals(typeName)) ? defaultType : SchemaHelper.getEntityClassForRawType(typeName));

					if (type == null) {
						logger.warn("Skipping node {}. Type cannot be found: {}!", id, typeName);

						missingTypesForImport.add(typeName);
					}

					final Map<String, Object> basicPropertiesMap = new HashMap();
					basicPropertiesMap.put("id", id);
					basicPropertiesMap.put("type", typeName);
					basicPropertiesMap.put("owner", entry.get("owner"));
					basicPropertiesMap.put("grantees", entry.get("grantees"));

					entry.remove("owner");
					entry.remove("grantees");

					final NodeInterface basicNode = app.create(type, PropertyMap.inputTypeToJavaType(context, type, basicPropertiesMap));

					correctNumberFormats(context, entry, type);

					final PropertyContainer pc = basicNode.getPropertyContainer();
					pc.setProperties(entry);
				}

				tx.success();

			} catch (FrameworkException fex) {

				logger.error("Unable to import nodes for type {}. Cause: {}", defaultType.getSimpleName(), fex.getMessage());
				fex.printStackTrace();
			}
		}
	}

	private enum DataType { Integer, Double, Long };

	private void correctNumberFormats(final SecurityContext context, final Map<String, Object> map, final Class type) throws FrameworkException {

		final List<GraphObjectMap> allProperties = SchemaHelper.getSchemaTypeInfo(context, type.getSimpleName(), type, "all");
		final Map<String, DataType> props        = new HashMap();

		for (final GraphObjectMap propertyInfo : allProperties) {

			final Map propInfo        = propertyInfo.toMap();
			final String propertyName = (String) propInfo.get("jsonName");
			final String propertyType = (String) propInfo.get("type");

			if ("Double".equals(propertyType)) {

				props.put(propertyName, DataType.Double);

			} else if ("Date".equals(propertyType) || "Long".equals(propertyType)) {

				props.put(propertyName, DataType.Long);

			} else if ("Integer".equals(propertyType)) {

				props.put(propertyName, DataType.Integer);
			}
		}

		for (Map.Entry<String, Object> entry : map.entrySet()) {

			final DataType propertyType = props.get(entry.getKey());
			final Object value          = entry.getValue();

			if (propertyType != null && value != null) {

				try {

					switch (propertyType) {

						case Double:
							// do nothing, GSON imports every Number as double
							break;

						case Integer:
							map.put(entry.getKey(), ((Double)value).intValue());
							break;

						case Long:
							map.put(entry.getKey(), ((Double)value).longValue());
							break;
					}

				} catch (ClassCastException cex) {

					logger.warn("Wrong data type for key {}, expected {}, got {}, ignoring.", entry.getKey(), propertyType.name(), value.getClass().getSimpleName());
				}
			}
		}
	}
}
