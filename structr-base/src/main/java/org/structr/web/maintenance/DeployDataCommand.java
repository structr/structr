/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.maintenance;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.schema.SchemaHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.maintenance.deploy.DeletingFileImportVisitor;
import org.structr.web.maintenance.deploy.ImportPreconditionFailedException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class DeployDataCommand extends DeployCommand {

	private static final Logger logger = LoggerFactory.getLogger(DeployDataCommand.class);

	private Set<Class> exportTypes;
	private Set<Class> exportFileAndFolderTypes;
	private Set<Class> missingTypesForExport;
	private Set<String> missingTypeNamesForExport;
	private Set<String> exportedFoldersAsParents;
	private Set<String> seenRelTypes;
	private Set<String> alreadyExportedRelationships;

	private Set<String> missingTypesForImport;
	private Map<String, Map<String, Integer>> failedRelationshipImports;

	private final static String DEPLOYMENT_DATA_IMPORT_NODE_DIRECTORY  = "nodes";
	private final static String DEPLOYMENT_DATA_IMPORT_RELS_DIRECTORY  = "relationships";

	private final static String DEPLOYMENT_DATA_IMPORT_STATUS      = "DEPLOYMENT_DATA_IMPORT_STATUS";
	private final static String DEPLOYMENT_DATA_EXPORT_STATUS      = "DEPLOYMENT_DATA_EXPORT_STATUS";

	public final static String DO_INNER_CALLBACKS_PARAMETER_NAME  = "doInnerCallbacks";
	public final static String DO_OUTER_CALLBACKS_PARAMETER_NAME  = "doOuterCallbacks";
	public final static String DO_CASCADING_DELETE_PARAMETER_NAME = "doCascadingDelete";

	private boolean doInnerCallbacks  = false;
	private boolean doOuterCallbacks  = false;
	private boolean doCascadingDelete = false;

	public static final String TYPE_NAME       = "typeName";
	public static final String MESSAGE_ID      = "messageId";
	public static final String PROGRESS        = "progress";
	public static final String CUR_CHUNK_TIME  = "curChunkTime";
	public static final String MEAN_CHUNK_TIME = "meanChunkTime";

	private static final String FILES_FILE_PARENTS_PATH = "file-parents.json";

	// is being handled via export of "grantees" and "owner" attributes
	private final static Set<String> blacklistedRelationshipTypes = Set.of("PrincipalOwnsNode", "Security");

	static {

		MaintenanceResource.registerMaintenanceCommand("deployData", DeployDataCommand.class);
	}

	@Override
	public void doExport(final Map<String, Object> parameters) throws FrameworkException {

		final String path  = (String) parameters.get("target");
		final String types = (String) parameters.get("types");

		if (StringUtils.isBlank(path)) {

			publishWarningMessage("Data export not started", "Please provide target path for data deployment export.");
			throw new FrameworkException(422, "Please provide target path for data deployment export.");
		}

		if (StringUtils.isBlank(types)) {

			publishWarningMessage("Data export not started", "Please provide a comma-separated list of type(s) to export.");
			throw new FrameworkException(422, "Please provide a comma-separated list of type(s) to export.");
		}

		final Path target  = Paths.get(path);

		if (target.isAbsolute() != true) {

			publishWarningMessage("Data export not started", "Target path '" + path + "' is not an absolute path - relative paths are not allowed.");
			throw new FrameworkException(422, "Target path '" + path + "' is not an absolute path - relative paths are not allowed.");
		}

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
			alreadyExportedRelationships = new HashSet();
			exportedFoldersAsParents     = new HashSet();
			seenRelTypes                 = new HashSet();

			Files.createDirectories(target);

			final Path preDataDeployConf  = target.resolve("pre-data-deploy.conf");
			final Path postDataDeployConf = target.resolve("post-data-deploy.conf");

			if (!Files.exists(preDataDeployConf)) {

				writeStringToFile(preDataDeployConf, "{\n\t// automatically created " + preDataDeployConf.getFileName() + ". This file is interpreted as a script and run before the data deployment process. To learn more about this, please have a look at the documentation.\n}");
			}

			if (!Files.exists(postDataDeployConf)) {

				writeStringToFile(postDataDeployConf, "{\n\t// automatically created " + postDataDeployConf.getFileName() + ". This file is interpreted as a script and run after the data deployment process. To learn more about this, please have a look at the documentation.\n}");
			}

			final Path nodesDir        = Files.createDirectories(target.resolve(DEPLOYMENT_DATA_IMPORT_NODE_DIRECTORY));
			final Path relsDir         = Files.createDirectories(target.resolve(DEPLOYMENT_DATA_IMPORT_RELS_DIRECTORY));
			final Path filesDir        = Files.createDirectories(target.resolve(FILES_FOLDER_PATH));
			final Path filesConfigFile = target.resolve(FILES_FILE_PATH);

			// clean up folders before export
			try {
				deleteDirectoryContentsRecursively(nodesDir);
			} catch (IOException ioe) {
				logger.warn("Unable to clean up {}: {}", nodesDir, ioe.getMessage());
			}
			try {
				deleteDirectoryContentsRecursively(relsDir);
			} catch (IOException ioe) {
				logger.warn("Unable to clean up {}: {}", relsDir, ioe.getMessage());
			}
			try {
				deleteDirectoryContentsRecursively(filesDir);
			} catch (IOException ioe) {
				logger.warn("Unable to clean up {}: {}", filesDir, ioe.getMessage());
			}

			// first determine all requested types
			for (final String trimmedType : types.split(",")) {

				final String typeName = trimmedType.trim();

				final Class type = SchemaHelper.getEntityClassForRawType(typeName);

				if (type == null) {

					logger.warn("Unable to export data for type '{}' - type unknown", typeName);
					publishWarningMessage("Type not found", "Unable to export data for type '" + typeName + "' - type unknown");

				} else {

					exportTypes.add(type);
				}
			}

			removeInheritingTypesFromExportSet(exportTypes);

			exportFileAndFolderTypes = exportTypes.stream().filter(aClass -> AbstractFile.class.isAssignableFrom(aClass)).collect(Collectors.toSet());
			exportTypes.removeAll(exportFileAndFolderTypes);

			final SecurityContext context = getRecommendedSecurityContext();

			exportFileAndFolderTypes(context, target, filesConfigFile, filesDir, relsDir);
			exportNodeTypes(context, nodesDir, relsDir);
			finalizeRelationshipFiles(relsDir);

			if (!missingTypeNamesForExport.isEmpty()) {

				final String title = "Possibly missing type(s) in export";
				final String text = "Relationships to/from the following type(s) were exported during <b>data deployment</b> but the type(s) (or supertype(s)) were not in the export set.<br>"
						+ "The affected relationships will only be able to be imported if the target/source is already present in the target instance.<br><br>"
						+ missingTypeNamesForExport.stream().sorted().collect(Collectors.joining(", "))
						+ "<br><br>You can safely ignore this if you are sure that the type is not required.";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tRelationships to/from the following type(s) were exported during data deployment but the type(s) (or supertype(s)) were not in the export set.\n"
						+ "\tThe affected relationships will only be able to be imported if the target/source is already present in the target instance.\n\n"
						+ "\t" + missingTypeNamesForExport.stream().sorted().collect(Collectors.joining(", "))
						+ "\n\n\tYou can safely ignore this if you are sure that the type is not required.\n"
						+ "###############################################################################"
				);

				publishWarningMessage(title, text);
			}

			if (exportedFoldersAsParents.size() > 0) {

				final String ftitle = "Folders added to export to represent file structure";
				final String ftext = "The following folders are not part of the configured export set, but were still included in the export to correctly represent the directory structure.<br>"
						+ "These folders will also be imported (if they do not exist).<br><br>"
						+ exportedFoldersAsParents.stream().sorted().collect(Collectors.joining("<br>"));

				logger.info("\n###############################################################################\n"
						+ "\tInfo: " + ftitle + "!\n"
						+ "\tThe following folders are not part of the configured export set, but were still included in the export to correctly represent the directory structure.\n"
						+ "\tThese folders will also be imported (if they do not exist).\n\n"
						+ "\t" + exportedFoldersAsParents.stream().sorted().collect(Collectors.joining("\n\t"))
						+ "\n###############################################################################"
				);

				publishInfoMessage(ftitle, ftext);
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
			ambiguousPrincipals.clear();

			final String path = (String) parameters.get("source");

			if (StringUtils.isBlank(path)) {

				throw new ImportPreconditionFailedException("Data Deployment Import not started", "Please provide 'source' attribute for deployment source directory path.");
			}

			final Path source = Paths.get(path);
			if (!Files.exists(source)) {

				throw new ImportPreconditionFailedException("Data Deployment Import not started", "Source path " + path + " does not exist.");
			}

			if (!Files.isDirectory(source)) {

				throw new ImportPreconditionFailedException("Data Deployment Import not started", "Source path " + path + " is not a directory.");
			}

			if (source.isAbsolute() != true) {

				throw new ImportPreconditionFailedException("Data Deployment Import not started", "Source path '" + path + "' is not an absolute path - relative paths are not allowed.");
			}

			doInnerCallbacks  = parameters.get(DO_INNER_CALLBACKS_PARAMETER_NAME) == null  ? false : "true".equals(parameters.get(DO_INNER_CALLBACKS_PARAMETER_NAME).toString());
			doOuterCallbacks  = parameters.get(DO_OUTER_CALLBACKS_PARAMETER_NAME) == null  ? false : "true".equals(parameters.get(DO_OUTER_CALLBACKS_PARAMETER_NAME).toString());
			doCascadingDelete = parameters.get(DO_CASCADING_DELETE_PARAMETER_NAME) == null ? false : "true".equals(parameters.get(DO_CASCADING_DELETE_PARAMETER_NAME).toString());

			doImportFromDirectory(source);

		} catch (ImportPreconditionFailedException ipfe) {

			logger.warn("{}: {}", ipfe.getTitle(), ipfe.getMessage());
			publishWarningMessage(ipfe.getTitle(), ipfe.getMessageHtml());

			setCommandStatusCode(422);
			setCustomCommandResult(ipfe.getTitle() + ": " + ipfe.getMessage());

		} catch (Throwable t) {

			final String title          = "Fatal Error";
			final String warningMessage = "Something went wrong - the deployment import has stopped. Please see the log for more information";

			publishWarningMessage(title, warningMessage);

			setCommandStatusCode(422);
			setCustomCommandResult(title + ": " + warningMessage);

			throw t;

		} finally {

			// restore saved value
			Settings.ChangelogEnabled.setValue(changeLogEnabled);
		}
	}

	public void doImportFromDirectory(final Path source) {

		missingTypesForImport     = new HashSet();
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

		if (Boolean.TRUE.equals(doOuterCallbacks)) {

			logger.info("You provided the 'doOuterCallbacks' parameter - if you encounter problems caused by afterCreate method you might want to disable this.");
		}

		if (Boolean.TRUE.equals(doCascadingDelete)) {

			logger.info("You provided the 'doCascadingDelete' parameter - if you encounter problems caused by nodes being deleted, you might want to disable this.");
			context.setDoCascadingDelete(true);
		}

		// apply pre-deploy.conf
		applyConfigurationFileIfExists(context, source.resolve("pre-data-deploy.conf"), DEPLOYMENT_DATA_IMPORT_STATUS);

		// do the actual import
		final Path nodesDir = source.resolve(DEPLOYMENT_DATA_IMPORT_NODE_DIRECTORY);
		final Path relsDir  = source.resolve(DEPLOYMENT_DATA_IMPORT_RELS_DIRECTORY);
		final Path filesDir = source.resolve(FILES_FOLDER_PATH);

		if (Files.exists(filesDir)) {

			try {

				importFiles(source, context);

			} catch(FrameworkException fxe) {

				logger.warn("Exception while importing files", fxe);
			}
		}

		if (Files.exists(nodesDir)) {

			try {

				Files.list(nodesDir).sorted().forEach((Path p) -> {

					java.io.File f = p.toFile();

					if (f.isFile() && f.getName().endsWith(".json")) {

						final String typeName = StringUtils.substringBeforeLast(f.getName(), ".json");

						importExtensibleNodeListData(context, typeName, readConfigList(p));
					}
				});

			} catch (IOException ex) {

				logger.warn("Exception while importing nodes", ex);
			}
		}

		if (Files.exists(relsDir)) {

			try {

				Files.list(relsDir).sorted().forEach((Path p) -> {

					java.io.File f = p.toFile();

					if (f.isFile() && f.getName().endsWith(".json")) {

						final String typeName = StringUtils.substringBeforeLast(f.getName(), ".json");

						final Class type = SchemaHelper.getEntityClassForRawType(typeName);

						if (type == null) {

							logger.warn("Not importing data. Relationship type cannot be found: {}!", typeName);
							publishWarningMessage(DEPLOYMENT_DATA_IMPORT_STATUS, "Type can not be found! NOT Importing relationships for type " + typeName);

						} else {

							importRelationshipListData(context, type, readConfigList(p));
						}
					}
				});

			} catch (IOException ex) {

				logger.warn("Exception while importing nodes", ex);
			}
		}

		// apply post-deploy.conf
		applyConfigurationFileIfExists(context, source.resolve("post-data-deploy.conf"), DEPLOYMENT_DATA_IMPORT_STATUS);

		if (!missingPrincipals.isEmpty()) {

			final String title = "Missing Principal(s)";
			final String text = "The following user(s) and/or group(s) are missing for resource access permissions or node ownership during <b>data deployment</b>.<br>"
					+ "Because of these missing permissions/ownerships, <b>node access rights are not identical to the export you just imported</b>."
					+ "<ul><li>" + missingPrincipals.stream().sorted().collect(Collectors.joining("</li><li>")) + "</li></ul>"
					+ "Consider adding these principals to your <a href=\"https://docs.structr.com/docs/fundamental-concepts#pre-deployconf\">pre-data-deploy.conf</a> and re-importing.";

			logger.info("\n###############################################################################\n"
					+ "\tWarning: " + title + "!\n"
					+ "\tThe following user(s) and/or group(s) are missing for resource access permissions or node ownership during deployment.\n"
					+ "\tBecause of these missing permissions/ownerships, node access rights are not identical to the export you just imported.\n\n"
					+ "\t" + missingPrincipals.stream().sorted().collect(Collectors.joining("\n\t"))
					+ "\n\n\tConsider adding these principals to your 'pre-data-deploy.conf' (see https://docs.structr.com/docs/fundamental-concepts#pre-deployconf) and re-importing.\n"
					+ "###############################################################################"
			);

			publishWarningMessage(title, text);
		}

		if (!ambiguousPrincipals.isEmpty()) {

			final String title = "Ambiguous Principal(s)";
			final String text = "For the following names, there are multiple candidates (User/Group) for resource access permissions or node ownership during <b>data deployment</b>.<br>"
					+ "Because of this ambiguity, <b>node access rights could not be restored as defined in the export you just imported</b>."
					+ "<ul><li>" + ambiguousPrincipals.stream().sorted().collect(Collectors.joining("</li><li>")) + "</li></ul>"
					+ "Consider clearing up such ambiguities in the database.";

			logger.info("\n###############################################################################\n"
					+ "\tWarning: " + title + "!\n"
					+ "\tFor the following names, there are multiple candidates (User/Group) for resource access permissions or node ownership during data deployment.\n"
					+ "\tBecause of this ambiguity, node access rights could not be restored as defined in the export you just imported.\n\n"
					+ "\t" + ambiguousPrincipals.stream().sorted().collect(Collectors.joining("\n\t"))
					+ "\n\n\tConsider clearing up such ambiguities in the database.\n"
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

			final String infos = "<ul>" + failedRelationshipImports.keySet().stream().reduce("", (tmp, typeName) -> {

				final Map<String, Integer> relInfo = failedRelationshipImports.get(typeName);

				return tmp + "<li>" + typeName + "<ul>" + relInfo.keySet().stream().reduce("", (innerTmp, missingNodeType) -> {

					final int value = relInfo.get(missingNodeType);

					if (value > 0) {
						return innerTmp + "<li>" + value + "x " + missingNodeType + " node missing</li>";
					}

					return innerTmp;

				}) + "</ul></li>";

			}) + "</ul>";

			final String title = "Failed Relationship(s) for Type(s)";
			final String text = "The following list shows the number of failed relationship imports per type.<br>"
					+ "Make sure you are importing the data to the correct schema and that the <strong>source node</strong> and <strong>target node</strong> of the relationship(s) are in the database/export! (See the log for more details)<br>"
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

	protected SecurityContext getRecommendedSecurityContext() {

		final SecurityContext context = SecurityContext.getSuperUserInstance();

		context.setDoTransactionNotifications(false);
		context.disablePreventDuplicateRelationships();
		context.disableModificationOfAccessTime();
		context.disableInnerCallbacks();
		context.setDoCascadingDelete(false);

		return context;
	}

	private void removeInheritingTypesFromExportSet(final Set<Class> types) {

		final Map<String, String> removedTypes = new HashMap();
		final HashSet<Class> clonedTypes       = new HashSet();

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

			logger.warn("The following types were removed from the export set because a parent type is already being exported:\n" + String.join("\n", messages));
			publishWarningMessage("Type(s) removed from export set", "The following types were removed from the export set because a <b>parent type is already being exported</b>:<br>" + String.join("<br>", messages));
		}
	}

	private void exportNodeTypes(final SecurityContext context, final Path nodesDir, final Path relsDir) throws FrameworkException {

		for (final Class type : exportTypes) {

			if (User.class.isAssignableFrom(type)) {

				logger.warn("User type in export set! Type '{}' is a User type.\n\tIf, on import, the user who is running the import is present in the import data, this can lead to problems.", type.getSimpleName());
				publishWarningMessage("User type in export set", "Type '" + type.getSimpleName() + "' is a User type.<br>If, on import, the user who is running the import is present in the import data, this can lead to problems.");
			}

			final Path typeConf = nodesDir.resolve(type.getSimpleName() + ".json");

			exportDataForNodeType(context, type, typeConf, relsDir);
		}
	}

	private void exportFileAndFolderTypes(final SecurityContext context, final Path target, final Path filesConfig, final Path filesDir, final Path relsDir) throws FrameworkException, IOException {

		final App app                                = StructrApp.getInstance(context);
		final Map<String, Object> filesAndFoldersMap = new TreeMap();
		final Gson gson                              = getGson();

		for (final Class fileOrFolderClass : exportFileAndFolderTypes) {

			final String simpleName  = fileOrFolderClass.getSimpleName();
			final String baseMessage = "Exporting nodes for type ";

			boolean hasMore = true;
			long nodeCount  = 0;
			int pageSize    = Settings.DeploymentNodeExportBatchSize.getValue();
			int page        = 1;
			long totalTime  = 0;

			publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, simpleName, TYPE_NAME, simpleName));
			logger.info("{}{}", baseMessage, simpleName);

			while (hasMore) {

				final long startTime = System.currentTimeMillis();

				try (final Tx tx = app.tx()) {

					final List<AbstractNode> files = app.nodeQuery(fileOrFolderClass).page(page).pageSize(pageSize).getAsList();
					hasMore = (files.size() == pageSize);

					for (final AbstractNode fileOrFolder : files) {

						if (fileOrFolder instanceof GenericNode) {

							logger.info("Not exporting database object because its schema type does not exist anymore: {}:{}", fileOrFolder.getType(), fileOrFolder.getUuid());

						} else {

							exportFileOrFolder((AbstractFile) fileOrFolder, filesAndFoldersMap, filesDir, true);

							exportRelationshipsForNode(context, fileOrFolder, relsDir);

							nodeCount++;
						}
					}
				}

				final long duration = (System.currentTimeMillis() - startTime);
				totalTime += duration;
				final float meanChunkTime = totalTime / page;

				publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, simpleName, TYPE_NAME, simpleName, PROGRESS, "(" + nodeCount + ")", CUR_CHUNK_TIME, duration, MEAN_CHUNK_TIME, meanChunkTime));
				logger.info("{}{} ({}) (chunk: {}s, mean:{}s)", baseMessage, simpleName, nodeCount, duration/1000.0, meanChunkTime/1000.0);

				page++;
			}
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(filesConfig.toFile()))) {

			fos.write(gson.toJson(filesAndFoldersMap));

		} catch (IOException ioex) {

			logger.warn("", ioex);
		}

		final Path requiredParentsPathsFile = target.resolve(FILES_FILE_PARENTS_PATH);
		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(requiredParentsPathsFile.toFile()))) {

			final List parents = new ArrayList(exportedFoldersAsParents);
			Collections.sort(parents);

			fos.write(gson.toJson(parents));

		} catch (IOException ioex) {

			logger.warn("", ioex);
		}

	}

	private void exportFileOrFolder(final AbstractFile fileOrFolder, final Map<String, Object> filesAndFoldersMap, final Path filesDir, final Boolean isDirectExport) throws IOException {

		if (fileOrFolder instanceof GenericNode) {

			logger.info("Not exporting database object because its schema type does not exist anymore: {}:{}", fileOrFolder.getType(), fileOrFolder.getUuid());
			return;
		}

		if (Boolean.TRUE.equals(fileOrFolder.getHasParent())) {

			final AbstractFile parent = fileOrFolder.getParent();

			if (parent == null) {

				logger.info("Not exporting parent folder for database object because can not be instantiated. This typically means that the schema type of the parent node does not exist anymore. Current node: {}:{}", fileOrFolder.getType(), fileOrFolder.getUuid());

			} else {

				// parents have to be exported, even if the given Folder type is not part of the export set
				exportFileOrFolder(parent, filesAndFoldersMap, filesDir, false);
			}
		}

		final String path             = fileOrFolder.getPath();
		final boolean alreadyExported = filesAndFoldersMap.containsKey(path);

		if (!alreadyExported) {

			final Path fileSystemPath = filesDir.resolve(path.substring(1));

			if (Folder.class.isAssignableFrom(fileOrFolder.getClass())) {

				Files.createDirectories(fileSystemPath);

				final Map<String, Object> properties = new TreeMap<>();
				exportFileConfiguration(fileOrFolder, properties);

				filesAndFoldersMap.put(path, properties);

			} else {

				exportFile(fileSystemPath.getParent(), (File)fileOrFolder, filesAndFoldersMap);
			}
		}

		// we export everything to files.json (even folders which are just exported as required parents)
		// but we remember the required parents to export them later
		if (isDirectExport == true) {

			exportedFoldersAsParents.remove(path);

		} else if (!alreadyExported) {

			exportedFoldersAsParents.add(path);
		}
	}

	private void exportDataForNodeType(final SecurityContext context, final Class<NodeInterface> nodeType, final Path targetConfFile, final Path relsDir) throws FrameworkException {

		final String simpleName  = nodeType.getSimpleName();
		final String baseMessage = "Exporting nodes for type ";

		publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, simpleName, TYPE_NAME, simpleName));

		final App app = StructrApp.getInstance(context);

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(targetConfFile.toFile()))) {

			fos.write("[");

			final Gson gson = getGson();
			long nodeCount  = 0;
			boolean hasMore = true;
			int pageSize    = Settings.DeploymentNodeExportBatchSize.getValue();
			int page        = 1;
			long totalTime  = 0;

			while (hasMore) {

				final long startTime = System.currentTimeMillis();

				try (final Tx tx = app.tx()) {

					final List<NodeInterface> list = app.nodeQuery(nodeType).pageSize(pageSize).page(page).getAsList();
					hasMore = (list.size() == pageSize);

					for (final NodeInterface node : list) {

						if (nodeCount > 0) {
							fos.write(",");
						}

						fos.write("\n");
						fos.write(gson.toJson(getMapRepresentationForNode(context, node)));

						nodeCount++;

						exportRelationshipsForNode(context, node, relsDir);
					}
				}

				final long duration = (System.currentTimeMillis() - startTime);
				totalTime += duration;
				final float meanChunkTime = totalTime / page;

				publishProgressMessage(DEPLOYMENT_DATA_EXPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, simpleName, TYPE_NAME, simpleName, PROGRESS, "(" + nodeCount + ")", CUR_CHUNK_TIME, duration, MEAN_CHUNK_TIME, meanChunkTime));
				logger.info("{}{} ({}) (chunk: {}s, mean:{}s)", baseMessage, simpleName, nodeCount, duration/1000.0, meanChunkTime/1000.0);

				page++;
			}

			if (nodeCount > 0) {

				fos.write("\n");
			}

			fos.write("]");

		} catch (IOException ioex) {

			logger.warn("", ioex);
		}
	}

	private Map<String, Object> getMapRepresentationForNode(final SecurityContext context, final NodeInterface node) {

		final Map<String, Object> entry = new TreeMap<>();
		final PropertyContainer pc      = node.getPropertyContainer();

		for (final String key : pc.getPropertyKeys()) {

			Object obj = pc.getProperty(key);

			// TODO: GSON can not serialize ZonedDateTime which would result in an exception => convert to string
			// TODO: use DatabaseConverter to convert it rather than doing this?
			if (obj instanceof ZonedDateTime) {
				obj = obj.toString();
			}

			putData(entry, key, obj);
		}

		exportOwnershipAndSecurity(node, entry);

		return entry;
	}

	private void exportRelationshipsForNode(final SecurityContext context, final NodeInterface node, final Path relsDir) throws FrameworkException {

		for (final RelationshipInterface rel : node.getRelationships()) {

			exportRelationship(context, rel, relsDir);
		}
	}

	private boolean isTypeInExportedTypes(final Class type) {

		for (final Class exportedType : exportTypes) {

			if (exportedType.isAssignableFrom(type)) {

				return true;
			}
		}

		for (final Class exportedType : exportFileAndFolderTypes) {

			if (exportedType.isAssignableFrom(type)) {

				return true;
			}
		}

		return false;
	}

	private void exportRelationship(final SecurityContext context, final RelationshipInterface rel, final Path relsDir) throws FrameworkException {

		final String relTypeName = rel.getClass().getSimpleName();

		if (!blacklistedRelationshipTypes.contains(relTypeName)) {

			final App app = StructrApp.getInstance(context);

			final String relUuid = rel.getUuid();

			if (!alreadyExportedRelationships.contains(relUuid)) {

				try (final Tx tx = app.tx()) {

					final NodeInterface sourceNode = rel.getSourceNode();
					final NodeInterface targetNode = rel.getTargetNode();

					if (!(sourceNode instanceof GenericNode) && !(targetNode instanceof GenericNode)) {

						final Map<String, Object> entry = new TreeMap<>();

						final Class sourceNodeClass = sourceNode.getClass();
						final Class targetNodeClass = targetNode.getClass();

						if (!missingTypesForExport.contains(sourceNodeClass) && !isTypeInExportedTypes(sourceNodeClass)) {

							missingTypeNamesForExport.add(sourceNodeClass.getSimpleName());
						}

						if (!missingTypesForExport.contains(targetNodeClass) && !isTypeInExportedTypes(targetNodeClass)) {

							missingTypeNamesForExport.add(targetNodeClass.getSimpleName());
						}

						final PropertyContainer pc = rel.getPropertyContainer();

						for (final String key : pc.getPropertyKeys()) {

							putData(entry, key, pc.getProperty(key));
						}

						entry.put("sourceId", rel.getSourceNodeId());
						entry.put("targetId", rel.getTargetNodeId());
						entry.put("relType",  rel.getProperty("relType"));

						exportRelationshipDirectly(rel.getClass().getSimpleName(), entry, relsDir);

						alreadyExportedRelationships.add(relUuid);
					}
				}
			}
		}
	}

	private void exportRelationshipDirectly(final String simpleRelType, final Map<String, Object> relInfo, final Path relsDir) {

		final Path relConf = relsDir.resolve(simpleRelType + ".json");

		final boolean relOfTypeAlreadyExported = seenRelTypes.contains(simpleRelType);

		if (relOfTypeAlreadyExported) {

			// add separator
			writeToFile(relConf, ",\n", true);

		} else {

			// add opening bracket
			writeToFile(relConf, "[\n", false);
		}

		seenRelTypes.add(simpleRelType);

		writeToFile(relConf, getGson().toJson(relInfo), true);
	}

	private void finalizeRelationshipFiles(final Path relsDir) {

		for (final String relType : seenRelTypes) {

			final Path p = relsDir.resolve(relType + ".json");

			// we know there must be at least one relationship in that file => finish with "\n]"
			writeToFile(p, "\n]", true);
		}
	}

	protected void writeToFile(final Path path, final String text, final boolean append) {

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(path.toFile(), append))) {

			fos.write(text);

		} catch (IOException ioex) {

			logger.warn("", ioex);
		}
	}

	private void importRelationshipListData(final SecurityContext context, final Class type, final List<Map<String, Object>> data) {

		final App app         = StructrApp.getInstance(context);
		final String typeName = type.getSimpleName();
		final int chunkSize   = Settings.DeploymentRelImportBatchSize.getValue();
		int chunkCount        = 0;
		int relCount          = 0;
		final int maxSize     = data.size();
		long totalTime        = 0;

		final String baseMessage = "Importing relationships for type ";
		publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, typeName, TYPE_NAME, typeName, PROGRESS, "(" + maxSize + ")"));
		logger.info("{}{} ({})", baseMessage, typeName, maxSize);

		while (data.size() >= (chunkCount * chunkSize)) {

			int endIndex = ((chunkCount + 1) * chunkSize);

			if (endIndex > maxSize) {
				endIndex = maxSize;
			}

			final List<Map<String, Object>> sublist = data.subList((chunkCount * chunkSize), endIndex);

			chunkCount++;

			final long startTime = System.currentTimeMillis();

			try (final Tx tx = app.tx(true, doOuterCallbacks)) {

				tx.disableChangelog();

				for (final Map<String, Object> entry : sublist) {

					final String sourceId = (String) entry.get("sourceId");
					final String targetId = (String) entry.get("targetId");

					final NodeInterface sourceNode = app.getNodeById(sourceId);
					final NodeInterface targetNode = app.getNodeById(targetId);

					if (sourceNode == null) {

						logger.error("Unable to import relationship of type {} with id {}. Source node not found! {}", typeName, entry.get("id"), sourceId);
						incrementFailedRelationshipsCounterForType(typeName, "source");
					}

					if (targetNode == null) {

						logger.error("Unable to import relationship of type {} with id {}. Target node not found! {}", typeName, entry.get("id"), targetId);
						incrementFailedRelationshipsCounterForType(typeName, "target");
					}

					if (sourceNode != null && targetNode != null) {

						final RelationshipInterface r = app.create(sourceNode, targetNode, type);

						correctNumberFormats(context, entry, type);

						final PropertyContainer pc = r.getPropertyContainer();
						pc.setProperties(entry);

						// finally, add affected graph objects to index
						r.addToIndex();
						sourceNode.addToIndex();
						targetNode.addToIndex();
					}
				}

				tx.success();

				relCount += sublist.size();

			} catch (FrameworkException fex) {

				logger.error("Unable to import relationships for type {}. Cause: {}", typeName, fex.toString());
				publishWarningMessage("Unable to import relationships for type " + typeName, fex.toString());
			}

			final long duration = (System.currentTimeMillis() - startTime);
			totalTime += duration;
			final float meanChunkTime = totalTime / chunkCount;

			publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, typeName, TYPE_NAME, typeName, PROGRESS, "(" + relCount + " / " + maxSize + ")", CUR_CHUNK_TIME, duration, MEAN_CHUNK_TIME, totalTime / chunkCount));
			logger.info("{}{} ({} / {}) (chunk: {}s, mean:{}s)", baseMessage, typeName, relCount, maxSize, duration/1000.0, meanChunkTime/1000.0);
		}
	}

	private void incrementFailedRelationshipsCounterForType (final String typeName, final String missingType) {

		if (!failedRelationshipImports.containsKey(typeName)) {

			failedRelationshipImports.put(typeName, new HashMap<String, Integer>() {{
				put("source", 0);
				put("target", 0);
			}});
		}

		failedRelationshipImports.get(typeName).put(missingType, failedRelationshipImports.get(typeName).get(missingType) + 1);
	}

	private void importFiles(final Path source, final SecurityContext ctx) throws FrameworkException {

		final Path filesMetadataFile = source.resolve(FILES_FILE_PATH);
		if (Files.exists(filesMetadataFile)) {

			final Map<String, Object> filesMetadata = readMetadataFileIntoMap(filesMetadataFile);

			final Path files = source.resolve(FILES_FOLDER_PATH);
			if (Files.exists(files)) {

				try {

					final String baseMessage = "Importing files and folders";
					final int batchSize      = Settings.DeploymentNodeImportBatchSize.getValue();
					final int maxSize        = filesMetadata.size();
					final String messageId   = "files-folders";

					publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, messageId, PROGRESS, "(" + maxSize + ")"));
					logger.info("{} ({})", baseMessage, maxSize);

					final Path requiredParentsPathsFile     = source.resolve(FILES_FILE_PARENTS_PATH);
					final List<String> requiredParentsPaths = readRequiredParentsFileIntoList(requiredParentsPathsFile);

					final DeletingFileImportVisitor fiv = new DeletingFileImportVisitor(ctx, files, filesMetadata, batchSize, requiredParentsPaths) {

						@Override
						protected void sendProgressUpdateNotification(final int count, final long chunkDuration, final long meanChunkDuration) {

							publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, messageId, PROGRESS, "(" + count + " / " + maxSize + ")", CUR_CHUNK_TIME, chunkDuration, MEAN_CHUNK_TIME, meanChunkDuration));
							logger.info("{} ({} / {}) (chunk: {}s, mean:{}s)", baseMessage, count, maxSize, chunkDuration/1000.0, meanChunkDuration/1000.0);
						}
					};
					Files.walkFileTree(files, fiv);
					fiv.finished();

				} catch (IOException ioex) {

					logger.warn("Exception while importing files", ioex);
				}
			}
		}
	}

	private void importExtensibleNodeListData(final SecurityContext context, final String defaultTypeName, final List<Map<String, Object>> data) {

		final Class defaultType = SchemaHelper.getEntityClassForRawType(defaultTypeName);

		if (defaultType == null) {

			logger.warn("Not importing data. Node type cannot be found: {}!", defaultTypeName);

			missingTypesForImport.add(defaultTypeName);

		} else {

			final App app       = StructrApp.getInstance(context);
			final int chunkSize = Settings.DeploymentNodeImportBatchSize.getValue();
			int chunkCount      = 0;
			int nodeCount       = 0;
			final int maxSize   = data.size();
			long totalTime      = 0;


			final String baseMessage = "Importing nodes for type ";
			publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, defaultTypeName, TYPE_NAME, defaultTypeName, PROGRESS, "(" + maxSize + ")"));
			logger.info("{}{} ({})", baseMessage, defaultTypeName, maxSize);

			while (data.size() >= (chunkCount * chunkSize)) {

				int endIndex = ((chunkCount + 1) * chunkSize);

				if (endIndex > maxSize) {
					endIndex = maxSize;
				}

				final List<Map<String, Object>> sublist = data.subList((chunkCount * chunkSize), endIndex);

				chunkCount++;

				final long startTime = System.currentTimeMillis();

				try (final Tx tx = app.tx(true, doOuterCallbacks)) {

					tx.disableChangelog();

					for (final Map<String, Object> entry : sublist) {

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

						} else {

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

							// finally, add node to index
							basicNode.addToIndex();
						}
					}

					tx.success();

					nodeCount += sublist.size();

				} catch (FrameworkException fex) {

					logger.error("Unable to import nodes for type {}. Cause: {}", defaultTypeName, fex.toString());
					publishWarningMessage("Unable to import nodes for type " + defaultTypeName, fex.toString());
				}

				final long duration = (System.currentTimeMillis() - startTime);
				totalTime += duration;
				final float meanChunkTime = totalTime / chunkCount;

				publishProgressMessage(DEPLOYMENT_DATA_IMPORT_STATUS, baseMessage, Map.of(MESSAGE_ID, defaultTypeName, TYPE_NAME, defaultTypeName, PROGRESS, "(" + nodeCount + " / " + maxSize + ")", CUR_CHUNK_TIME, duration, MEAN_CHUNK_TIME, meanChunkTime));
				logger.info("{}{} ({} / {}) (chunk: {}s, mean:{}s)", baseMessage, defaultTypeName, nodeCount, maxSize, duration/1000.0, meanChunkTime/1000.0);

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

					logger.warn("Wrong data type for key '{}', expected {}, got {}, ignoring this property for {}", entry.getKey(), propertyType.name(), value.getClass().getSimpleName(), map.get("id"));
				}
			}
		}
	}

	public List<String> readRequiredParentsFileIntoList(final Path metadataFile) {

		if (Files.exists(metadataFile)) {

			try (final Reader reader = Files.newBufferedReader(metadataFile, Charset.forName("utf-8"))) {

				return new ArrayList<>(getGson().fromJson(reader, ArrayList.class));

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}

		return new ArrayList<>();
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return true;
	}
}
