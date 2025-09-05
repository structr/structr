/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.VersionHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.*;
import org.structr.core.graph.*;
import org.structr.core.property.CypherProperty;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.core.traits.relationships.SecurityRelationshipDefinition;
import org.structr.module.StructrModule;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.*;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.AbstractMapComparator;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.File;
import org.structr.web.entity.*;
import org.structr.web.entity.dom.*;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.maintenance.deploy.*;
import org.structr.web.traits.definitions.*;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.structr.websocket.command.CreateComponentCommand;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitResult.CONTINUE;

public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger                     = LoggerFactory.getLogger(DeployCommand.class.getName());

	private int statusCode      = HttpServletResponse.SC_OK;
	private Object customResult = null;

	private static final Map<String, String> deferredPageLinks        = new LinkedHashMap<>();
	private final Map<DOMNode, PropertyMap> deferredNodesAndTheirProperties = new LinkedHashMap<>();

	protected static final Map<String, Integer> missingPrincipals   = new LinkedHashMap();
	protected static final Map<String, Integer> ambiguousPrincipals = new LinkedHashMap();
	protected static final Set<String> missingSchemaFile            = new HashSet<>();
	protected static final Set<String> deferredLogTexts             = new HashSet<>();

	protected static final AtomicBoolean deploymentActive      = new AtomicBoolean(false);

	private final static String DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_KEY          = "visibility-flags-relative-to";
	private final static String DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_PARENT_VALUE = "parent";
	private final static String DEPLOYMENT_VERSION_KEY                                  = "structr-version";
	private final static String DEPLOYMENT_UUID_FORMAT_KEY                              = "uuid-format";

	private final static String DEPLOYMENT_IMPORT_STATUS   = "DEPLOYMENT_IMPORT_STATUS";
	private final static String DEPLOYMENT_EXPORT_STATUS   = "DEPLOYMENT_EXPORT_STATUS";

	private final static String DEPLOYMENT_SCHEMA_GLOBAL_METHODS_FOLDER  = "_globalMethods";
	private final static String DEPLOYMENT_SCHEMA_METHODS_FOLDER         = "methods";
	private final static String DEPLOYMENT_SCHEMA_FUNCTIONS_FOLDER       = "functions";
	private final static String DEPLOYMENT_SCHEMA_READ_FUNCTION_SUFFIX   = ".readFunction";
	private final static String DEPLOYMENT_SCHEMA_WRITE_FUNCTION_SUFFIX  = ".writeFunction";
	private final static String DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY   = "source";

	private final static String DEPLOYMENT_CONF_FILE_PATH                             = "deployment.conf";
	private final static String PRE_DEPLOY_CONF_FILE_PATH                             = "pre-deploy.conf";
	private final static String POST_DEPLOY_CONF_FILE_PATH                            = "post-deploy.conf";
	private final static String SCHEMA_GRANTS_FILE_PATH                               = "security/schema-grants.json";
	private final static String GRANTS_FILE_PATH                                      = "security/grants.json";
	private final static String CORS_SETTINGS_FILE_PATH                               = "security/cors-settings.json";
	private final static String MAIL_TEMPLATES_FILE_PATH                              = "mail-templates.json";
	private final static String WIDGETS_FILE_PATH                                     = "widgets.json";
	private final static String LOCALIZATIONS_FILE_PATH                               = "localizations.json";
	private final static String APPLICATION_CONFIGURATION_DATA_FILE_PATH              = "application-configuration-data.json";
	protected final static String FILES_FILE_PATH                                     = "files.json";
	private final static String PAGES_FILE_PATH                                       = "pages.json";
	private final static String COMPONENTS_FILE_PATH                                  = "components.json";
	private final static String TEMPLATES_FILE_PATH                                   = "templates.json";
	private final static String ACTION_MAPPING_FILE_PATH                              = "events/action-mapping.json";
	private final static String PARAMETER_MAPPING_FILE_PATH                           = "events/parameter-mapping.json";
	private final static String SITES_FILE_PATH                                       = "sites.json";
	private final static String PAGE_PATHS_FILE_PATH                                  = "page-paths.json";
	private final static String SCHEMA_FOLDER_PATH                                    = "schema";
	private final static String COMPONENTS_FOLDER_PATH                                = "components";
	protected final static String FILES_FOLDER_PATH                                   = "files";
	private final static String PAGES_FOLDER_PATH                                     = "pages";
	private final static String SECURITY_FOLDER_PATH                                  = "security";
	private final static String TEMPLATES_FOLDER_PATH                                 = "templates";
	private final static String EVENTS_FOLDER_PATH                                    = "events";
	private final static String MODULES_FOLDER_PATH                                   = "modules";
	private final static String MAIL_TEMPLATES_FOLDER_PATH                            = "mail-templates";

	static {

		MaintenanceResource.registerMaintenanceCommand("deploy", DeployCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> parameters) throws FrameworkException {

		final String mode = (String) parameters.get("mode");

		if (Boolean.FALSE.equals(isDeploymentActive())) {

			try {

				deploymentActive.set(true);

				if ("export".equals(mode)) {

					doExport(parameters);

				} else if ("import".equals(mode)) {

					doImport(parameters);

				} else {

					logger.warn("Unsupported mode '{}'", mode);
				}

			} finally {

				deploymentActive.set(false);
			}

		} else {

			logger.warn("Prevented deployment '{}' while another deployment is active.", mode);
			publishWarningMessage("Prevented deployment '" + mode + "'", "Another deployment is currently active. Please wait until it is finished.");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	public Map<String, Object> readMetadataFileIntoMap(final Path metadataFile) {

		if (Files.exists(metadataFile)) {

			try (final Reader reader = Files.newBufferedReader(metadataFile, StandardCharsets.UTF_8)) {

				return new HashMap<>(getGson().fromJson(reader, Map.class));

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}

		return new HashMap<>();
	}

//	public StreamingJsonWriter getJsonWriter() {
//		return new StreamingJsonWriter(PropertyView.All, true, 1, false, true);
//	}

	public Gson getGson() {
		return new GsonBuilder().setPrettyPrinting().setDateFormat(Settings.DefaultDateFormat.getValue()).serializeNulls().create();
	}

	public static boolean isUuid(final String name) {
		return Settings.isValidUuid(name);
	}

	/**
	 * returns UUID from end of string depending on the UUID configuration
	 * does explicitly NOT work if the whole string is a UUID because that should have been checked before via isUuid(name)
	 */
	public static String getUuidOrNullFromEndOfString(final String name) {

		final String configuredUUIDv4Format = Settings.UUIDv4AllowedFormats.getValue();

		if (configuredUUIDv4Format.equals(Settings.POSSIBLE_UUID_V4_FORMATS.with_dashes.toString()) || configuredUUIDv4Format.equals(Settings.POSSIBLE_UUID_V4_FORMATS.both.toString())) {

			if (name.length() > 36) {

				final String last36Characters = name.substring(name.length() - 36);

				if (DeployCommand.isUuid(last36Characters)) {

					return last36Characters;
				}
			}
		}

		if (configuredUUIDv4Format.equals(Settings.POSSIBLE_UUID_V4_FORMATS.without_dashes.toString()) || configuredUUIDv4Format.equals(Settings.POSSIBLE_UUID_V4_FORMATS.both.toString())) {

			if (name.length() > 32) {

				final String last32Characters = name.substring(name.length() - 32);

				if (DeployCommand.isUuid(last32Characters)) {

					return last32Characters;
				}
			}
		}

		return null;
	}

	public static boolean isDeploymentActive() {
		return deploymentActive.get();
	}

	protected void doImport(final Map<String, Object> attributes) throws FrameworkException {

		// backup previous value of change log setting and disable during deployment
		final boolean changeLogEnabled = Settings.ChangelogEnabled.getValue();
		Settings.ChangelogEnabled.setValue(false);

		try {

			missingPrincipals.clear();
			ambiguousPrincipals.clear();
			missingSchemaFile.clear();
			deferredLogTexts.clear();

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final boolean extendExistingApp = isTrue(attributes.get("extendExistingApp"));
			final String path               = (String) attributes.get("source");
			final SecurityContext ctx       = SecurityContext.getSuperUserInstance();
			final App app                   = StructrApp.getInstance(ctx);

			ctx.setDoTransactionNotifications(false);
			ctx.disablePreventDuplicateRelationships();
			ctx.disableModificationOfAccessTime();
			ctx.setDoIndexing(false);

			if (StringUtils.isBlank(path)) {

				throw new ImportPreconditionFailedException("Please provide 'source' attribute for deployment source directory path.");
			}

			final Path source = Paths.get(path);
			if (!Files.exists(source)) {

				throw new ImportPreconditionFailedException("Source path " + path + " does not exist.");
			}

			if (!Files.isDirectory(source)) {

				throw new ImportPreconditionFailedException("Source path " + path + " is not a directory.");
			}

			if (!source.isAbsolute()) {

				throw new ImportPreconditionFailedException("Source path '" + path + "' is not an absolute path - relative paths are not allowed.");
			}

			// Define all files/folders beforehand
			final Path deploymentConfFile                       = source.resolve(DEPLOYMENT_CONF_FILE_PATH);
			final Path preDeployConfFile                        = source.resolve(PRE_DEPLOY_CONF_FILE_PATH);
			final Path postDeployConfFile                       = source.resolve(POST_DEPLOY_CONF_FILE_PATH);
			final Path schemaGrantsMetadataFile                 = source.resolve(SCHEMA_GRANTS_FILE_PATH);
			final Path grantsMetadataFile                       = source.resolve(GRANTS_FILE_PATH);
			final Path corsSettingsMetadataFile                 = source.resolve(CORS_SETTINGS_FILE_PATH);
			final Path mailTemplatesMetadataFile                = source.resolve(MAIL_TEMPLATES_FILE_PATH);
			final Path widgetsMetadataFile                      = source.resolve(WIDGETS_FILE_PATH);
			final Path localizationsMetadataFile                = source.resolve(LOCALIZATIONS_FILE_PATH);
			final Path applicationConfigurationDataMetadataFile = source.resolve(APPLICATION_CONFIGURATION_DATA_FILE_PATH);
			final Path filesMetadataFile                        = source.resolve(FILES_FILE_PATH);
			final Path pagesMetadataFile                        = source.resolve(PAGES_FILE_PATH);
			final Path componentsMetadataFile                   = source.resolve(COMPONENTS_FILE_PATH);
			final Path templatesMetadataFile                    = source.resolve(TEMPLATES_FILE_PATH);
			final Path actionMappingMetadataFile                = source.resolve(ACTION_MAPPING_FILE_PATH);
			final Path parameterMappingMetadataFile             = source.resolve(PARAMETER_MAPPING_FILE_PATH);
			final Path sitesConfFile                            = source.resolve(SITES_FILE_PATH);
			final Path pathsConfFile                            = source.resolve(PAGE_PATHS_FILE_PATH);
			final Path schemaFolder                             = source.resolve(SCHEMA_FOLDER_PATH);

			if (
				!Files.exists(deploymentConfFile) &&
				!Files.exists(preDeployConfFile) &&
				!Files.exists(postDeployConfFile) &&
				!Files.exists(grantsMetadataFile) &&
				!Files.exists(corsSettingsMetadataFile) &&
				!Files.exists(mailTemplatesMetadataFile) &&
				!Files.exists(widgetsMetadataFile) &&
				!Files.exists(localizationsMetadataFile) &&
				!Files.exists(applicationConfigurationDataMetadataFile) &&
				!Files.exists(filesMetadataFile) &&
				!Files.exists(pagesMetadataFile) &&
				!Files.exists(componentsMetadataFile) &&
				!Files.exists(templatesMetadataFile) &&
				!Files.exists(actionMappingMetadataFile) &&
				!Files.exists(parameterMappingMetadataFile) &&
				!Files.exists(sitesConfFile) &&
				!Files.exists(pathsConfFile) &&
				!Files.exists(schemaFolder)
			) {

				throw new ImportPreconditionFailedException("Source path '" + path + "' does not contain any of the files for a structr deployment.");
			}

			logger.info("Importing from '{}'", path);

			// read deployment.conf (file containing information about deployment export)
			final Map<String, String> deploymentConf = readDeploymentConfigurationFile(deploymentConfFile);
			final boolean relativeVisibility         = isDOMNodeVisibilityRelativeToParent(deploymentConf);

			checkDeploymentExportVersionIsCompatible(deploymentConf);
			checkDeploymentExportUUIDFormatIsCompatible(deploymentConf);

			final String message = "Read deployment config file '" + deploymentConfFile + "': " + deploymentConf.size() + " entries.";
			logger.info(message);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, message);

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("start",   startTime);
			broadcastData.put("source",  source.toString());
			publishBeginMessage(DEPLOYMENT_IMPORT_STATUS, broadcastData);

			// apply pre-deploy.conf
			applyConfigurationFileIfExists(ctx, preDeployConfFile, DEPLOYMENT_IMPORT_STATUS);

			importResourceAccessGrants(grantsMetadataFile);
			importCorsSettings(corsSettingsMetadataFile);
			importMailTemplates(mailTemplatesMetadataFile, source);
			importWidgets(widgetsMetadataFile);
			importLocalizations(localizationsMetadataFile);
			importApplicationConfigurationNodes(applicationConfigurationDataMetadataFile);
			importSchema(schemaFolder, extendExistingApp);
			importSchemaGrants(schemaGrantsMetadataFile);

			final FileImportVisitor.FileImportProblems fileImportProblems = importFiles(filesMetadataFile, source, ctx);

			importHTMLContent(app, source, pagesMetadataFile, componentsMetadataFile, templatesMetadataFile, sitesConfFile, pathsConfFile, extendExistingApp, relativeVisibility, deferredNodesAndTheirProperties);
			linkDeferredPages(app);
			importParameterMapping(parameterMappingMetadataFile);
			importActionMapping(actionMappingMetadataFile);
			importEmbeddedApplicationData(source);

			// import modules (including flow) after everything else so the DOMNode -> Flow-Relationship can be imported
			importModuleData(source);

			// apply post-deploy.conf
			applyConfigurationFileIfExists(ctx, postDeployConfFile, DEPLOYMENT_IMPORT_STATUS);

			// migrate imported app
			MigrationService.execute();

			if (!missingPrincipals.isEmpty()) {

				final String title = "Missing Principal(s)";
				final String text = "The following user(s) and/or group(s) are missing for resource access permissions or node ownership during <b>deployment</b>.<br>"
						+ "Because of these missing permissions/ownerships, <b>the functionality is not identical to the export you just imported</b>."
						+ "<ul><li>" + transformCountedMapToHumanReadableList(missingPrincipals, "</li><li>") + "</li></ul>"
						+ "Consider adding these principals to your <a href=\"https://docs.structr.com/docs/fundamental-concepts#pre-deployconf\">pre-deploy.conf</a> and re-importing.";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tThe following user(s) and/or group(s) are missing for resource access permissions or node ownership during deployment.\n"
						+ "\tBecause of these missing permissions/ownerships, the functionality is not identical to the export you just imported.\n\n"
						+ "\t" + transformCountedMapToHumanReadableList(missingPrincipals, "\n\t")
						+ "\n\n\tConsider adding these principals to your 'pre-deploy.conf' (see https://docs.structr.com/docs/fundamental-concepts#pre-deployconf) and re-importing.\n"
						+ "###############################################################################"
				);
				publishWarningMessage(title, text);
			}

			if (!ambiguousPrincipals.isEmpty()) {

				final String title = "Ambiguous Principal(s)";
				final String text = "For the following names, there are multiple candidates (User/Group) for resource access permissions or node ownership during <b>deployment</b>.<br>"
						+ "Because of this ambiguity, <b>node access rights could not be restored as defined in the export you just imported</b>."
						+ "<ul><li>" + transformCountedMapToHumanReadableList(ambiguousPrincipals, "</li><li>") + "</li></ul>"
						+ "Consider clearing up such ambiguities in the database.";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tFor the following names, there are multiple candidates (User/Group) for resource access permissions or node ownership during deployment.\n"
						+ "\tBecause of this ambiguity, node access rights could not be restored as defined in the export you just imported.\n\n"
						+ "\t" + transformCountedMapToHumanReadableList(ambiguousPrincipals, "\n\t")
						+ "\n\n\tConsider clearing up such ambiguities in the database.\n"
						+ "###############################################################################"
				);
				publishWarningMessage(title, text);
			}

			if (!missingSchemaFile.isEmpty()) {

				final String title = "Missing Schema file(s)";
				final String text = "The following schema methods/functions require file(s) to be present in the tree-based schema export.<br>"
						+ "Because those files are missing, the functionality will not be available after importing.<br>"
						+ "The most common cause is that someone forgot to add these files to the repository."
						+ "<ul><li>" + missingSchemaFile.stream().sorted().collect(Collectors.joining("</li><li>")) + "</li></ul>";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tThe following schema methods/functions require file(s) to be present in the tree-based schema export.\n"
						+ "\tBecause those files are missing, the functionality will not be available after importing.\n"
						+ "\tThe most common cause is that someone forgot to add these files to the repository.\n\n"
						+ "\t" + missingSchemaFile.stream().sorted().collect(Collectors.joining("\n\t"))
						+ "\n###############################################################################"
				);
				publishWarningMessage(title, text);
			}

			if (fileImportProblems != null && fileImportProblems.hasAnyProblems()) {

				final String title = "Encountered problems during import of files";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ fileImportProblems.getProblemsText()
						+ "\n###############################################################################"
				);
				publishWarningMessage(title, fileImportProblems.getProblemsHtml());
			}

			final long endTime = System.currentTimeMillis();
			DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

			customHeaders.put("end", new Date(endTime).toString());
			customHeaders.put("duration", duration);

			logger.info("Import from {} done. (Took {})", source, duration);

			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			publishEndMessage(DEPLOYMENT_IMPORT_STATUS, broadcastData);

		} catch (ImportPreconditionFailedException ipfe) {

			logger.warn("{}: {}", ipfe.getTitle(), ipfe.getMessage());
			publishWarningMessage(ipfe.getTitle(), ipfe.getMessageHtml());

			setCommandStatusCode(422);
			setCustomCommandResult(ipfe.getTitle() + ": " + ipfe.getMessage());

		} catch (FrameworkException fex) {

			final String title          = "Fatal Error";
			final String warningMessage = "Something went wrong - the deployment import has stopped. Please see the log for more information.<br><br>" + fex;

			publishWarningMessage(title, warningMessage);

			setCommandStatusCode(422);
			setCustomCommandResult(title + ": " + warningMessage);

			throw fex;

		} catch (Throwable t) {

			final String title          = "Fatal Error";
			final String warningMessage = "Something went wrong - the deployment import has stopped. Please see the log for more information.";

			publishWarningMessage(title, warningMessage);

			setCommandStatusCode(422);
			setCustomCommandResult(title + ": " + warningMessage);

			throw t;

		} finally {

			// log collected warnings at the end, so they do not get lost
			for (final String logText : deferredLogTexts) {
				logger.info(logText);
			}

			// restore saved value
			Settings.ChangelogEnabled.setValue(changeLogEnabled);
		}
	}

	protected void doExport(final Map<String, Object> attributes) throws FrameworkException {

		final String path = (String) attributes.get("target");

		if (StringUtils.isBlank(path)) {

			publishWarningMessage("Export not started", "Please provide target path for deployment export.");

			throw new FrameworkException(422, "Please provide target path for deployment export.");
		}

		final Path target  = Paths.get(path);

		if (!target.isAbsolute()) {

			publishWarningMessage("Export not started", "Target path '" + path + "' is not an absolute path - relative paths are not allowed.");

			throw new FrameworkException(422, "Target path '" + path + "' is not an absolute path - relative paths are not allowed.");
		}

		try {

			deferredLogTexts.clear();

			Files.createDirectories(target);

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("start",  startTime);
			broadcastData.put("target", target.toString());
			publishBeginMessage(DEPLOYMENT_EXPORT_STATUS, broadcastData);

			final Path components          = Files.createDirectories(target.resolve(COMPONENTS_FOLDER_PATH));
			final Path files               = Files.createDirectories(target.resolve(FILES_FOLDER_PATH));
			final Path pages               = Files.createDirectories(target.resolve(PAGES_FOLDER_PATH));
			final Path schemaFolder        = Files.createDirectories(target.resolve(SCHEMA_FOLDER_PATH));
			final Path security            = Files.createDirectories(target.resolve(SECURITY_FOLDER_PATH));
			final Path templates           = Files.createDirectories(target.resolve(TEMPLATES_FOLDER_PATH));
			final Path events              = Files.createDirectories(target.resolve(EVENTS_FOLDER_PATH));
			final Path modules             = Files.createDirectories(target.resolve(MODULES_FOLDER_PATH));
			final Path mailTemplatesFolder = Files.createDirectories(target.resolve(MAIL_TEMPLATES_FOLDER_PATH));

			final Path schemaGrantsConf                    = target.resolve(SCHEMA_GRANTS_FILE_PATH);
			final Path grantsConf                          = target.resolve(GRANTS_FILE_PATH);
			final Path corsSettingsConf                    = target.resolve(CORS_SETTINGS_FILE_PATH);
			final Path filesConf                           = target.resolve(FILES_FILE_PATH);
			final Path sitesConf                           = target.resolve(SITES_FILE_PATH);
			final Path pagesConf                           = target.resolve(PAGES_FILE_PATH);
			final Path pathsConf                           = target.resolve(PAGE_PATHS_FILE_PATH);
			final Path componentsConf                      = target.resolve(COMPONENTS_FILE_PATH);
			final Path templatesConf                       = target.resolve(TEMPLATES_FILE_PATH);
			final Path mailTemplatesConf                   = target.resolve(MAIL_TEMPLATES_FILE_PATH);
			final Path localizationsConf                   = target.resolve(LOCALIZATIONS_FILE_PATH);
			final Path widgetsConf                         = target.resolve(WIDGETS_FILE_PATH);
			final Path actionMappingConf                   = target.resolve(ACTION_MAPPING_FILE_PATH);
			final Path parameterMappingConf                = target.resolve(PARAMETER_MAPPING_FILE_PATH);
			final Path deploymentConfFile                  = target.resolve(DEPLOYMENT_CONF_FILE_PATH);
			final Path applicationConfigurationData        = target.resolve(APPLICATION_CONFIGURATION_DATA_FILE_PATH);

			final Path preDeployConf            = target.resolve(PRE_DEPLOY_CONF_FILE_PATH);
			final Path postDeployConf           = target.resolve(POST_DEPLOY_CONF_FILE_PATH);

			if (!Files.exists(preDeployConf)) {

				writeStringToFile(preDeployConf, """
				{
					// This file was auto-generated. You may adapt it to suit your specific needs.
					// During the application deployment import process, this file is treated as a script and executed *before* any other actions take place.
					//
					// Important: because this script runs before the application schema is imported, it operates on the existing (current) schema.
					//
					// Its purpose is to ensure that all required users and groups are present before the application import occurs.
					// All operations in this script should be **idempotent** — meaning they can be safely run multiple times without causing unintended side effects.
					// For example, prefer using methods like `get_or_create` rather than `create` to avoid duplicate entries.
					//
					// For more information, please refer to the documentation.
				}""");
			}

			if (!Files.exists(postDeployConf)) {

				writeStringToFile(postDeployConf, """
				{
					// This file was auto-generated. You may adapt it to suit your specific needs.
					// During the application deployment import process, this file is treated as a script and executed *after* all other operations have finished.
					//
					// For more information, please refer to the documentation.
				}""");
			}

			writeDeploymentConfigurationFile(deploymentConfFile);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Files");
			exportFiles(files, filesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Sites");
			exportSites(sitesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Page Paths");
			exportPagePaths(pathsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Parameter Mapping");
			exportParameterMapping(parameterMappingConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Action Mapping");
			exportActionMapping(actionMappingConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Pages");
			exportPages(pages, pagesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Components");
			exportComponents(components, componentsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Templates");
			exportTemplates(templates, templatesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Schema Permissions");
			exportSchemaGrants(schemaGrantsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Resource Access Permissions");
			exportResourceAccessGrants(grantsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting CORS Settings");
			exportCorsSettings(corsSettingsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Schema");
			exportSchema(schemaFolder);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Mail Templates");
			exportMailTemplates(mailTemplatesConf, mailTemplatesFolder);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Localizations");
			exportLocalizations(localizationsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Widgets");
			exportWidgets(widgetsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Application Configuration Data");
			exportApplicationConfigurationData(applicationConfigurationData);

			for (StructrModule module : StructrApp.getConfiguration().getModules().values()) {

				if (module.hasDeploymentData()) {
					logger.info("Exporting deployment data for module {}", module.getName());

					publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting deployment data for module " + module.getName());

					final Path moduleFolder = Files.createDirectories(modules.resolve(module.getName()));
					module.exportDeploymentData(moduleFolder, getGson());
				}

			}

			// set group grants for created files
			final String groupName = Settings.DeploymentFileGroupName.getValue("");
			if (StringUtils.isNotBlank(groupName)) {
				setFileGroupRecursively(groupName, target);
			}

			// config import order is "users, grants, pages, components, templates"
			// data import order is "schema, files, templates, components, pages"

			logger.info("Export finished.");

			final long endTime = System.currentTimeMillis();
			DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

			customHeaders.put("end", new Date(endTime).toString());
			customHeaders.put("duration", duration);

			logger.info("Export to {} done. (Took {})", target, duration);

			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			publishEndMessage(DEPLOYMENT_EXPORT_STATUS, broadcastData);


		} catch (FileAlreadyExistsException faee) {

			final String deploymentTargetIsAFileError = "A file already exists at given path - this should be a directory or not exist at all!";

			logger.warn(deploymentTargetIsAFileError + target);

			publishWarningMessage("Fatal Error", deploymentTargetIsAFileError + "<br>" + target);

		} catch (IOException ex) {

			logger.warn("", ex);

		} finally {

			// log collected warnings at the end so they dont get lost
			for (final String logText : deferredLogTexts) {

				logger.info(logText);
			}
		}
	}

	private void setFileGroupRecursively(String groupName, Path target) throws IOException {

		try {

			final UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
			final GroupPrincipal group                     = lookupService.lookupPrincipalByGroupName(groupName);

			Files.walkFileTree(target, new GroupAddFileVisitor(group));

		} catch (UnsupportedOperationException ex) {

			publishWarningMessage("Unable to set group ownership", "The filesystem you are writing to does not have a user/group lookup service. A group named '" + groupName + "' can not be looked up. The deployment export files will not have that group association.");

			logger.warn("The filesystem you are writing to does not have a user/group lookup service. A group named '{}' can not be looked up. The deployment export files will not have that group association.", groupName);

		} catch (UserPrincipalNotFoundException ex) {

			publishWarningMessage("Unable to set group ownership", "A group named '" + groupName + "' was not found. The deployment export files will not have that group association.");

			logger.warn("Unable to set group ownership", "A group named '{}' was not found. The deployment export files will not have that group association.", groupName);

		} catch (Exception ex) {

			publishWarningMessage("Unable to set group ownership", "An error occurred trying to look up a group named '" + groupName + "'. The deployment export files will not have that group association. See server log for more details.");

			logger.warn("An error occurred trying to look up a group named '{}'. The deployment export files will not have that group association. Error detail follows:", groupName);

			ex.printStackTrace();
		}
	}

	private void exportFiles(final Path target, final Path configTarget) throws FrameworkException {

		logger.info("Exporting files (unchanged files will be skipped)");

		final Traits traits                 = Traits.of(StructrTraits.FILE);
		final PropertyKey<Boolean> inclKey  = traits.key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY);
		final PropertyKey<Folder> parentKey = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final Map<String, Object> config    = new TreeMap<>();
		final App app                       = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// fetch toplevel folders and recurse
			for (final NodeInterface folder : app.nodeQuery(StructrTraits.FOLDER).key(parentKey, null).sort(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).and().key(inclKey, true).getAsList()) {
				exportFilesAndFolders(target, folder, config);
			}

			// fetch toplevel files that are marked for export
			for (final NodeInterface file : app.nodeQuery(StructrTraits.FILE)
				.sort(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY))
				.and()
					.key(parentKey, null)
					.key(inclKey, true)
				.getAsList()) {

				exportFile(target, file, config);
			}

			tx.success();

		} catch (IOException ioex) {

			logger.warn("", ioex);
		}

		try {

			logger.info("Cleaning up files");
			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Cleaning up files");

			FileCleanupVisitor fiv = new FileCleanupVisitor(target, config);
			Files.walkFileTree(target, fiv);

		} catch (IOException ioex) {

			logger.warn("Exception while cleaning up files", ioex);
		}

		writeJsonToFile(configTarget, config);
	}

	private void exportFilesAndFolders(final Path target, final NodeInterface node, final Map<String, Object> config) throws IOException {

		final Folder folder = node.as(Folder.class);

		// ignore folders with mounted content
		if (folder.isMounted()) {
			return;
		}

		final Traits traits                  = Traits.of(StructrTraits.FOLDER);
		final String name                    = folder.getName();
		final Path path                      = target.resolve(name);
		final Map<String, Object> properties = new TreeMap<>();

		Files.createDirectories(path);

		exportFileConfiguration(node, properties);

		if (!properties.isEmpty()) {
			String folderPath = folder.getPath();
			config.put(folderPath, properties);
		}

		final List<Folder> folders    = Iterables.toList(folder.getFolders());
		final PropertyKey<String> key = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final Comparator comp         = key.sorted(false);

		Collections.sort(folders, comp);

		for (final Folder child : folders) {

			exportFilesAndFolders(path, child, config);
		}

		final List<File> files = Iterables.toList(folder.getFiles());
		Collections.sort(files, comp);

		for (final File file : files) {

			exportFile(path, file, config);
		}
	}

	protected void exportFile(final Path target, final NodeInterface node, final Map<String, Object> config) throws IOException {

		final File file                      = node.as(File.class);
		final Map<String, Object> properties = new TreeMap<>();
		final String name                    = file.getName();
		Path targetPath                      = target.resolve(name);
		boolean doExport                     = true;

		if (Files.exists(targetPath)) {

			// compare checksum
			final Long checksumOfExistingFile = FileHelper.getChecksum(targetPath.toFile());
			final Long checksumOfExportFile   = file.getChecksum();

			doExport = !checksumOfExistingFile.equals(checksumOfExportFile);
		}

		if (doExport) {

			try {

				IOUtils.copy(file.getRawInputStream(), new FileOutputStream(targetPath.toFile()));

			} catch (IOException ioex) {

				logger.warn("Unable to write file {}: {}", targetPath, ioex.getMessage());
			}
		}

		exportFileConfiguration(node, properties);

		if (!properties.isEmpty()) {

			String filePath = file.getPath();
			config.put(filePath, properties);
		}
	}

	private void exportSites(final Path target) throws FrameworkException {

		logger.info("Exporting sites");

		final List<Map<String, Object>> sites = new LinkedList<>();
		final App app                          = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.SITE).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final Site site                 = node.as(Site.class);
				final Map<String, Object> entry = new TreeMap<>();
				sites.add(entry);

				entry.put(GraphObjectTraitDefinition.ID_PROPERTY,                             site.getUuid());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                         site.getName());
				entry.put(SiteTraitDefinition.HOSTNAME_PROPERTY,                              site.getHostname());
				entry.put(SiteTraitDefinition.PORT_PROPERTY,                                  site.getPort());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, site.isVisibleToAuthenticatedUsers());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        site.isVisibleToPublicUsers());

				final List<String> pageNames = new LinkedList<>();

				for (final NodeInterface page : site.getPages()) {
					pageNames.add(page.getName());
				}

				entry.put(SiteTraitDefinition.PAGES_PROPERTY, pageNames);

				exportOwnershipAndSecurity(node, entry);
			}

			tx.success();
		}

		writeJsonToFile(target, sites);
	}

	private void exportPagePaths(final Path target) throws FrameworkException {

		logger.info("Exporting page paths");

		final List<Map<String, Object>> paths = new LinkedList<>();
		final App app                         = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.PAGE_PATH).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final PagePath path             = node.as(PagePath.class);
				final Map<String, Object> entry = new TreeMap<>();

				paths.add(entry);

				entry.put(GraphObjectTraitDefinition.ID_PROPERTY,                             path.getUuid());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                         path.getName());
				entry.put(PagePathTraitDefinition.PRIORITY_PROPERTY,                          path.getPriority());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, path.isVisibleToAuthenticatedUsers());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        path.isVisibleToPublicUsers());
				entry.put(PagePathTraitDefinition.PAGE_PROPERTY,                              Map.of("id", path.getPage().getUuid()));

				final List<Map<String, Object>> parameters = new LinkedList<>();

				for (final NodeInterface parameterNode : path.getParameters()) {

					final PagePathParameter parameter = parameterNode.as(PagePathParameter.class);
					final Map<String, Object> data    = new TreeMap<>();

					parameters.add(data);

					data.put(GraphObjectTraitDefinition.ID_PROPERTY, parameter.getUuid());

					if (parameter.getValueType() != null) {
						data.put(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY, parameter.getValueType());
					}

					if (parameter.getDefaultValue() != null) {
						data.put(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY, parameter.getDefaultValue());
					}

					if (parameter.getPosition() != null) {
						data.put(PagePathParameterTraitDefinition.POSITION_PROPERTY, parameter.getPosition());
					}

					data.put(PagePathParameterTraitDefinition.IS_OPTIONAL_PROPERTY,   parameter.getIsOptional());
				}

				entry.put(PagePathTraitDefinition.PARAMETERS_PROPERTY, parameters);

				exportOwnershipAndSecurity(node, entry);
			}

			tx.success();
		}

		writeJsonToFile(target, paths);
	}

	private void exportPages(final Path targetFolder, final Path configTarget) throws FrameworkException {

		logger.info("Exporting pages");

		try {
			deleteDirectoryContentsRecursively(targetFolder);
		} catch (IOException ioe) {
			logger.warn("Unable to clean up {}: {}", targetFolder, ioe.getMessage());
		}

		final Map<String, Object> pagesConfig = new TreeMap<>();
		final App app                         = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface page : app.nodeQuery(StructrTraits.PAGE).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				if (!page.is(StructrTraits.SHADOW_DOCUMENT)) {

					final String content = page.as(Page.class).getContent(RenderContext.EditMode.DEPLOYMENT);
					if (content != null) {

						final Map<String, Object> properties = new TreeMap<>();
						final String name                    = page.getName();
						final Path pageFile                  = targetFolder.resolve(name + ".html");

						pagesConfig.put(name, properties);
						exportConfiguration(page, properties);
						exportOwnershipAndSecurity(page, properties);

						writeStringToFile(pageFile, content);
					}
				}
			}

			tx.success();
		}

		writeJsonToFile(configTarget, pagesConfig);
	}

	private void exportComponents(final Path targetFolder, final Path configTarget) throws FrameworkException {

		logger.info("Exporting components");

		try {
			deleteDirectoryContentsRecursively(targetFolder);
		} catch (IOException ioe) {
			logger.warn("Unable to clean up {}: {}", targetFolder, ioe.getMessage());
		}

		final Map<String, Object> configuration = new TreeMap<>();
		final App app                           = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final NodeInterface shadowDocument = app.nodeQuery(StructrTraits.SHADOW_DOCUMENT).getFirst();
			if (shadowDocument != null) {

				for (final DOMNode node : shadowDocument.as(ShadowDocument.class).getElements()) {

					final boolean hasParent = node.getParent() != null;
					final boolean inTrash   = node.inTrash();

					// skip nodes in trash and non-toplevel nodes
					if (inTrash || hasParent) {
						continue;
					}

					final String content = node.getContent(RenderContext.EditMode.DEPLOYMENT);

					exportContentElementSource(targetFolder, node, configuration, content);
				}
			}

			tx.success();
		}

		writeJsonToFile(configTarget, configuration);
	}

	private void exportTemplates(final Path targetFolder, final Path configTarget) throws FrameworkException {

		logger.info("Exporting templates");

		try {
			deleteDirectoryContentsRecursively(targetFolder);
		} catch (IOException ioe) {
			logger.warn("Unable to clean up {}: {}", targetFolder, ioe.getMessage());
		}

		final Map<String, Object> configuration = new TreeMap<>();
		final App app                           = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// export template nodes anywhere in the pages tree which are not related to shared components
			for (final NodeInterface node : app.nodeQuery(StructrTraits.TEMPLATE).getAsList()) {

				final Template template = node.as(Template.class);
				final boolean isShared  = template.hasSharedComponent();
				final boolean inTrash   = template.inTrash();

				if (inTrash || isShared) {
					continue;
				}

				final String content = template.getContent();

				exportContentElementSource(targetFolder, node, configuration, content);
			}

			tx.success();
		}

		writeJsonToFile(configTarget, configuration);
	}

	/**
	 * Consolidated export method for Content and Template
	 */
	private void exportContentElementSource(final Path targetFolder, final NodeInterface node, final Map<String, Object> configuration, final String content) throws FrameworkException {

		if (content != null) {

			// name with uuid or just uuid
			String name = node.getProperty(node.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			if (name != null) {

				name += "-" + node.getUuid();

			} else {

				name = node.getUuid();
			}

			final Map<String, Object> properties = new TreeMap<>();
			final Path targetFile = targetFolder.resolve(name + ".html");

			configuration.put(name, properties);
			exportConfiguration(node, properties);

			writeStringToFile(targetFile, content);
		}
	}

	private void exportSchemaGrants(final Path target) throws FrameworkException {

		logger.info("Exporting schema permissions");

		final List<Map<String, Object>> grants = new LinkedList<>();
		final Traits traits                    = Traits.of(StructrTraits.SCHEMA_GRANT);
		final App app                          = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_GRANT).sort(traits.key(GraphObjectTraitDefinition.ID_PROPERTY)).getAsList()) {

				final Map<String, Object> grant = new TreeMap<>();
				final SchemaGrant schemaGrant   = node.as(SchemaGrant.class);
				grants.add(grant);

				grant.put(GraphObjectTraitDefinition.ID_PROPERTY,                          schemaGrant.getUuid());
				grant.put(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY,                   Map.of("name", schemaGrant.getPrincipalName()));
				grant.put(SchemaGrantTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY,     schemaGrant.getStaticSchemaNodeName());
				grant.put(SchemaGrantTraitDefinition.ALLOW_READ_PROPERTY,                  schemaGrant.allowRead());
				grant.put(SchemaGrantTraitDefinition.ALLOW_WRITE_PROPERTY,                 schemaGrant.allowWrite());
				grant.put(SchemaGrantTraitDefinition.ALLOW_DELETE_PROPERTY,                schemaGrant.allowDelete());
				grant.put(SchemaGrantTraitDefinition.ALLOW_ACCESS_CONTROL_PROPERTY,        schemaGrant.allowAccessControl());

				// schema node can be null
				final SchemaNode optionalSchemaNode = schemaGrant.getSchemaNode();
				if (optionalSchemaNode != null) {

					grant.put(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY, Map.of("name", optionalSchemaNode.getName()));
				}
			}

			tx.success();
		}

		writeSortedCompactJsonToFile(target, grants, null);
	}

	private void exportResourceAccessGrants(final Path target) throws FrameworkException {

		logger.info("Exporting resource access permissions");

		final List<Map<String, Object>> grants = new LinkedList<>();
		final Traits traits                    = Traits.of(StructrTraits.RESOURCE_ACCESS);
		final PropertyKey<String> signatureKey = traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY);
		final PropertyKey<Long> flagsKey       = traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY);
		final App app                          = StructrApp.getInstance();

		final List<String> unreachableGrants = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface res : app.nodeQuery(StructrTraits.RESOURCE_ACCESS).sort(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY)).getAsList()) {

				final Map<String, Object> grant = new TreeMap<>();
				grants.add(grant);

				grant.put(GraphObjectTraitDefinition.ID_PROPERTY,                             res.getProperty(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY)));
				grant.put(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY,                   res.getProperty(signatureKey));
				grant.put(ResourceAccessTraitDefinition.FLAGS_PROPERTY,                       res.getProperty(flagsKey));
				grant.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        res.isVisibleToPublicUsers());
				grant.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, res.isVisibleToAuthenticatedUsers());

				exportSecurity(res, grant);

				final List grantees = (List)grant.get(NodeInterfaceTraitDefinition.GRANTEES_PROPERTY);

				if (res.getProperty(flagsKey) > 0 && !res.isVisibleToPublicUsers() && !res.isVisibleToAuthenticatedUsers() && grantees.isEmpty()) {
					unreachableGrants.add(res.getProperty(signatureKey));
				}
			}

			tx.success();
		}

		if (!unreachableGrants.isEmpty()) {

			final String text = "Found configured but unreachable permission(s)! The ability to use group/user rights to permissions has been added to improve flexibility.\n\n  The following permissions are inaccessible for any non-admin users:\n\n"
					+ unreachableGrants.stream().reduce( "", (acc, signature) -> acc.concat("  - ").concat(signature).concat("\n"))
					+ "\n  You can edit the visibility in the 'Security' area.\n";

			final String htmlText = "The ability to use group/user rights to permissions has been added to improve flexibility. The following permissions are inaccessible for any non-admin users:<br><br>"
					+ unreachableGrants.stream().reduce( "", (acc, signature) -> acc.concat("&nbsp;- ").concat(signature).concat("<br>"))
					+ "<br>You can edit the visibility in the <a href=\"#security\">Security</a> area.";

			deferredLogTexts.add(text);
			publishWarningMessage("Found configured but unreachable permission(s)", htmlText);
		}

		writeSortedCompactJsonToFile(target, grants, null);
	}

	private void exportCorsSettings(final Path target) throws FrameworkException {

		logger.info("Exporting CORS Settings");

		final List<Map<String, Object>> corsSettings = new LinkedList<>();
		final Traits traits                          = Traits.of(StructrTraits.CORS_SETTING);
		final App app                                = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface corsSetting : app.nodeQuery(StructrTraits.CORS_SETTING).sort(traits.key(CorsSettingTraitDefinition.REQUEST_URI_PROPERTY)).getAsList()) {

				final Map<String, Object> entry = new LinkedHashMap<>();
				corsSettings.add(entry);

				putData(entry, GraphObjectTraitDefinition.ID_PROPERTY,                corsSetting.getProperty(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY)));
				putData(entry, CorsSettingTraitDefinition.REQUEST_URI_PROPERTY,       corsSetting.getProperty(traits.key(CorsSettingTraitDefinition.REQUEST_URI_PROPERTY)));
				putData(entry, CorsSettingTraitDefinition.ACCEPTED_ORIGINS_PROPERTY,  corsSetting.getProperty(traits.key(CorsSettingTraitDefinition.ACCEPTED_ORIGINS_PROPERTY)));
				putData(entry, CorsSettingTraitDefinition.MAX_AGE_PROPERTY,           corsSetting.getProperty(traits.key(CorsSettingTraitDefinition.MAX_AGE_PROPERTY)));
				putData(entry, CorsSettingTraitDefinition.ALLOW_METHODS_PROPERTY,     corsSetting.getProperty(traits.key(CorsSettingTraitDefinition.ALLOW_METHODS_PROPERTY)));
				putData(entry, CorsSettingTraitDefinition.ALLOW_HEADERS_PROPERTY,     corsSetting.getProperty(traits.key(CorsSettingTraitDefinition.ALLOW_HEADERS_PROPERTY)));
				putData(entry, CorsSettingTraitDefinition.ALLOW_CREDENTIALS_PROPERTY, corsSetting.getProperty(traits.key(CorsSettingTraitDefinition.ALLOW_CREDENTIALS_PROPERTY)));
				putData(entry, CorsSettingTraitDefinition.EXPOSE_HEADERS_PROPERTY,    corsSetting.getProperty(traits.key(CorsSettingTraitDefinition.EXPOSE_HEADERS_PROPERTY)));
			}

			tx.success();
		}

		writeSortedCompactJsonToFile(target, corsSettings, null);
	}

	private void exportSchema(final Path targetFolder) throws FrameworkException {

		logger.info("Exporting schema");

		try {

			// first delete all contents of the schema directory
			deleteDirectoryContentsRecursively(targetFolder);

			final StructrSchemaDefinition schema = (StructrSchemaDefinition)StructrSchema.createFromDatabase(StructrApp.getInstance());

			if (Settings.SchemaDeploymentFormat.getValue().equals("tree")) {

				// move user-defined functions to files
				final List<Map<String, Object>> userDefinedFunctions = schema.getUserDefinedFunctions();

				if (!userDefinedFunctions.isEmpty()) {

					final Path globalMethodsFolder = Files.createDirectories(targetFolder.resolve(DEPLOYMENT_SCHEMA_GLOBAL_METHODS_FOLDER));

					for (Map<String, Object> schemaMethod : userDefinedFunctions) {

						final String methodName            = (String) schemaMethod.get("name");

						final String methodSource          = (String) schemaMethod.get(DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY);
						final Path globalMethodSourceFile  = globalMethodsFolder.resolve(methodName);

						final String relativeSourceFilePath  = "./" + targetFolder.relativize(globalMethodSourceFile);

						schemaMethod.put(DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY, relativeSourceFilePath);

						if (Files.exists(globalMethodSourceFile)) {
							logger.warn("File '{}' already exists - this can happen if there is a non-unique global method definition. This is not supported in tree-based schema export and will causes errors!", relativeSourceFilePath);
						}

						if (methodSource != null) {
							writeStringToFile(globalMethodSourceFile, methodSource);
						}
					}
				}

				// move all methods/function properties to files
				for (final StructrTypeDefinition typeDef : schema.getTypeDefinitions()) {

					final String typeName = typeDef.getName();

					final List<StructrFunctionProperty> functionProperties = new LinkedList();
					for (final Object propDef : typeDef.getProperties()) {

						if (propDef instanceof StructrFunctionProperty) {
							functionProperties.add((StructrFunctionProperty)propDef);
						}
					}

					final boolean hasFunctionProperties = !functionProperties.isEmpty();
					final boolean hasMethods            = !typeDef.getMethods().isEmpty();

					if (hasFunctionProperties || hasMethods) {

						final Path typeFolder = Files.createDirectories(targetFolder.resolve(typeName));

						if (hasFunctionProperties) {

							final Path functionsFolder = Files.createDirectories(typeFolder.resolve(DEPLOYMENT_SCHEMA_FUNCTIONS_FOLDER));

							for (final StructrFunctionProperty fp : functionProperties) {

								final Path readFunctionFile  = functionsFolder.resolve(fp.getName() + DEPLOYMENT_SCHEMA_READ_FUNCTION_SUFFIX);
								final String readFunction    = fp.getReadFunction();

								if (readFunction != null) {
									writeStringToFile(readFunctionFile, readFunction);
									fp.setReadFunction("./" + targetFolder.relativize(readFunctionFile));
								}

								final Path writeFunctionFile = functionsFolder.resolve(fp.getName() + DEPLOYMENT_SCHEMA_WRITE_FUNCTION_SUFFIX);
								final String writeFunction   = fp.getWriteFunction();

								if (writeFunction != null) {
									writeStringToFile(writeFunctionFile, writeFunction);
									fp.setWriteFunction("./" + targetFolder.relativize(writeFunctionFile));
								}
							}
						}

						if (hasMethods) {

							final Path methodsFolder = Files.createDirectories(typeFolder.resolve(DEPLOYMENT_SCHEMA_METHODS_FOLDER));

							for (final Object m : typeDef.getMethods()) {

								final StructrMethodDefinition method = (StructrMethodDefinition)m;

								final String uniqueMethodName = method.getUniqueName();
								final String methodSource     = method.getSource();
								final Path methodSourceFile   = methodsFolder.resolve(uniqueMethodName);

								if (methodSource != null) {

									writeStringToFile(methodSourceFile, methodSource);
									method.setSource("./" + targetFolder.relativize(methodSourceFile));
								}
							}
						}
					}
				}
			}

			final Path schemaJson = targetFolder.resolve("schema.json");

			writeStringToFile(schemaJson, schema.toString());

		} catch (Throwable t) {
			logger.error("", t);
		}
	}

	private void exportConfiguration(final NodeInterface node, final Map<String, Object> config) throws FrameworkException {

		putData(config, GraphObjectTraitDefinition.ID_PROPERTY,                             node.getUuid());
		putData(config, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        node.isVisibleToPublicUsers());
		putData(config, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, node.isVisibleToAuthenticatedUsers());

		if (node.is(StructrTraits.CONTENT)) {
			putData(config, ContentTraitDefinition.CONTENT_TYPE_PROPERTY, node.as(Content.class).getContentType());
		}

		if (node.is(StructrTraits.TEMPLATE)) {

			final Template template = node.as(Template.class);

			// mark this template as being shared
			putData(config, "shared", Boolean.toString(template.isSharedComponent() && template.getParent() == null));
		}

		if (node.is(StructrTraits.PAGE)) {

			final Linkable linkable = node.as(Linkable.class);
			final Page page         = node.as(Page.class);

			putData(config, LinkableTraitDefinition.BASIC_AUTH_REALM_PROPERTY,  linkable.getBasicAuthRealm());
			putData(config, PageTraitDefinition.CACHE_FOR_SECONDS_PROPERTY,     page.getCacheForSeconds());
			putData(config, PageTraitDefinition.CATEGORY_PROPERTY,              page.getCategory());
			putData(config, PageTraitDefinition.CONTENT_TYPE_PROPERTY,          page.getContentType());
			putData(config, DOMNodeTraitDefinition.DONT_CACHE_PROPERTY,         page.dontCache());
			putData(config, LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY, linkable.getEnableBasicAuth());
			putData(config, NodeInterfaceTraitDefinition.HIDDEN_PROPERTY,       page.isHidden());
			putData(config, PageTraitDefinition.PAGE_CREATES_RAW_DATA_PROPERTY, page.pageCreatesRawData());
			putData(config, DOMElementTraitDefinition.PATH_PROPERTY,            page.getPath());
			putData(config, PageTraitDefinition.POSITION_PROPERTY,              page.getPosition());
			putData(config, PageTraitDefinition.SHOW_ON_ERROR_CODES_PROPERTY,   page.getShowOnErrorCodes());

			// FIXME? show conditions for a page?
			putData(config, DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY,    page.getShowConditions());
			putData(config, DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY,    page.getHideConditions());
		}

		final Traits traits = node.getTraits();

		// export all dynamic properties
		for (final PropertyKey key : traits.getAllPropertyKeys()) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (key.isDynamic() && key.relatedType() == null && !(key instanceof FunctionProperty) && !(key instanceof CypherProperty)) {

				putData(config, key.jsonName(), node.getProperty(key));
			}
		}
	}

	protected void exportFileConfiguration(final NodeInterface node, final Map<String, Object> config) {

		final AbstractFile abstractFile = node.as(AbstractFile.class);
		final String fileType           = abstractFile.getType();

		putData(config, GraphObjectTraitDefinition.ID_PROPERTY,                             abstractFile.getUuid());
		putData(config, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        abstractFile.isVisibleToPublicUsers());
		putData(config, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, abstractFile.isVisibleToAuthenticatedUsers());
		putData(config, GraphObjectTraitDefinition.TYPE_PROPERTY,                           fileType);

		if (abstractFile.is(StructrTraits.FILE)) {

			final File file = abstractFile.as(File.class);

			putData(config, FileTraitDefinition.IS_TEMPLATE_PROPERTY,               file.isTemplate());
			putData(config, FileTraitDefinition.DONT_CACHE_PROPERTY,                file.dontCache());
			putData(config, FileTraitDefinition.CONTENT_TYPE_PROPERTY,              file.getContentType());
			putData(config, FileTraitDefinition.CACHE_FOR_SECONDS_PROPERTY,         file.getCacheForSeconds());
		}

		putData(config, AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY, abstractFile.includeInFrontendExport(false));

		if (abstractFile.is(StructrTraits.LINKABLE)) {

			final Linkable linkable = abstractFile.as(Linkable.class);

			putData(config, LinkableTraitDefinition.BASIC_AUTH_REALM_PROPERTY,  linkable.getBasicAuthRealm());
			putData(config, LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY, linkable.getEnableBasicAuth());
		}

		if (abstractFile.is(StructrTraits.IMAGE)) {

			final Image image = abstractFile.as(Image.class);

			putData(config, ImageTraitDefinition.IS_THUMBNAIL_PROPERTY, image.isThumbnail());
			putData(config, ImageTraitDefinition.IS_IMAGE_PROPERTY,     image.isImage());
			putData(config, ImageTraitDefinition.WIDTH_PROPERTY,        image.getWidth());
			putData(config, ImageTraitDefinition.HEIGHT_PROPERTY,       image.getHeight());
		}

		final Traits traits = node.getTraits();

		// export all dynamic properties
		for (final PropertyKey key : traits.getAllPropertyKeys()) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (key.isDynamic() && key.relatedType() == null && !(key instanceof FunctionProperty) && !(key instanceof CypherProperty)) {

				putData(config, key.jsonName(), abstractFile.getProperty(key));
			}
		}

		exportOwnershipAndSecurity(node, config);
	}

	protected void exportOwnershipAndSecurity(final NodeInterface node, final Map<String, Object> config) {

		// export owner
		final Map<String, Object> map = new HashMap<>();
		final Principal owner         = node.as(AccessControllable.class).getOwnerNode();

		if (owner != null) {

			map.put("name", owner.getName());
			config.put("owner", map);

		} else {

			// export "null" owner as well
			config.put("owner", null);
		}

		exportSecurity(node, config);
	}

	protected void exportSecurity(final NodeInterface node, final Map<String, Object> config) {

		// export security grants
		final List<Map<String, Object>> grantees = new LinkedList<>();
		for (final Security security : node.as(AccessControllable.class).getSecurityRelationships()) {

			if (security != null) {

				final Set<String> allowedActions = security.getPermissions();
				final Map<String, Object> grant  = new TreeMap<>();

				grant.put(NodeInterfaceTraitDefinition.NAME_PROPERTY, security.getRelationship().getSourceNode().getName());
				grant.put(SecurityRelationshipDefinition.ALLOWED_PROPERTY, allowedActions);

				if (!allowedActions.isEmpty()) {
					grantees.add(grant);
				}
			}
		}

		// export empty grantees as well
		config.put("grantees", grantees);
	}

	public static void checkOwnerAndSecurity(final Map<String, Object> entry) throws FrameworkException {
		checkOwnerAndSecurity(entry, true);
	}

	public static void checkOwnerAndSecurity(final Map<String, Object> entry, final boolean removeNullOwner) throws FrameworkException {

		if (entry.containsKey("owner")) {

			final Map ownerData = ((Map)entry.get("owner"));
			if (ownerData != null) {

				final String ownerName               = (String) ownerData.get("name");
				final List<NodeInterface> principals = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).name(ownerName).getAsList();

				if (principals.isEmpty()) {

					DeployCommand.encounteredMissingPrincipal("Unknown owner", ownerName);

					entry.remove("owner");

				} else if (principals.size() > 1) {

					DeployCommand.encounteredAmbiguousPrincipal("Ambiguous owner", ownerName, principals.size());

					entry.remove("owner");
				}

			} else if (removeNullOwner) {

				entry.remove("owner");
			}
		}

		if (entry.containsKey("grantees")) {

			final List<Map<String, Object>> grantees        = (List) entry.get("grantees");
			final List<Map<String, Object>> cleanedGrantees = new LinkedList();

			for (final Map<String, Object> grantee : grantees) {

				final String granteeName             = (String) grantee.get("name");
				final List<NodeInterface> principals = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).name(granteeName).getAsList();

				if (principals.isEmpty()) {

					DeployCommand.encounteredMissingPrincipal("Unknown grantee", granteeName);

				} else if (principals.size() > 1) {

					DeployCommand.encounteredAmbiguousPrincipal("Ambiguous grantee", granteeName, principals.size());

				} else {

					cleanedGrantees.add(grantee);
				}

				// convert allowed string from old format (must be an array)
				if (grantee.containsKey("allowed")) {

					final Object value = grantee.get("allowed");
					if (value instanceof String s) {

						grantee.put("allowed", StringUtils.split(s, ","));
					}
				}
			}

			entry.put("grantees", cleanedGrantees);
		}

		// new section for schema grants
		if (entry.containsKey("principal")) {

			final Map principalData = ((Map)entry.get("principal"));
			if (principalData != null) {

				final String principalName           = (String) principalData.get("name");
				final List<NodeInterface> principals = StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).name(principalName).getAsList();

				if (principals.isEmpty()) {

					DeployCommand.encounteredMissingPrincipal("Unknown principal", principalName);

					entry.remove("principal");

				} else if (principals.size() > 1) {

					DeployCommand.encounteredAmbiguousPrincipal("Ambiguous principal", principalName, principals.size());

					entry.remove("principal");
				}

			} else if (removeNullOwner) {

				entry.remove("principal");
			}
		}
	}

	private void exportMailTemplates(final Path targetConf, final Path targetFolder) throws FrameworkException {

		logger.info("Exporting mail templates");

		final List<Map<String, Object>> mailTemplates = new LinkedList<>();
		final Traits traits                           = Traits.of(StructrTraits.MAIL_TEMPLATE);
		final App app                                 = StructrApp.getInstance();

		try {

			deleteDirectoryContentsRecursively(targetFolder);

			try (final Tx tx = app.tx()) {

				for (final NodeInterface node : app.nodeQuery(StructrTraits.MAIL_TEMPLATE).sort(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

					final MailTemplate mailTemplate = node.as(MailTemplate.class);

					// generate filename for output file
					String filename = mailTemplate.getName() + "_-_" + mailTemplate.getLocale() + ".html";

					if (Files.exists(targetFolder.resolve(filename))) {
						filename = mailTemplate.getName() + "_-_" + mailTemplate.getLocale() + "_-_" + mailTemplate.getUuid() + ".html";
					}

					final Map<String, Object> entry = new TreeMap<>();
					mailTemplates.add(entry);

					putData(entry, GraphObjectTraitDefinition.ID_PROPERTY,                            mailTemplate.getUuid());
					putData(entry, NodeInterfaceTraitDefinition.NAME_PROPERTY,                        mailTemplate.getName());
					putData(entry, "filename",                    filename);
					putData(entry, MailTemplateTraitDefinition.LOCALE_PROPERTY,                      mailTemplate.getLocale());
					putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, mailTemplate.isVisibleToAuthenticatedUsers());
					putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        mailTemplate.isVisibleToPublicUsers());

					final Path mailTemplateFile = targetFolder.resolve(filename);
					writeStringToFile(mailTemplateFile, mailTemplate.getText());
				}

				tx.success();
			}

		} catch (Throwable t) {
			logger.error("", t);
		}

		mailTemplates.sort(new AbstractMapComparator<Object>() {

			@Override
			public String getKey (final Map<String, Object> map) {

				// null values are auto-casted to "null" string
				return "" + map.get("name") + map.get("locale");
			}
		});

		writeJsonToFile(targetConf, mailTemplates);
	}

	private void exportWidgets(final Path target) throws FrameworkException {

		logger.info("Exporting widgets");

		final List<Map<String, Object>> widgets = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.WIDGET).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final Widget widget             = node.as(Widget.class);
				final Map<String, Object> entry = new TreeMap<>();

				widgets.add(entry);

				putData(entry, GraphObjectTraitDefinition.ID_PROPERTY,                             widget.getUuid());
				putData(entry, NodeInterfaceTraitDefinition.NAME_PROPERTY,                         widget.getName());
				putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, widget.isVisibleToAuthenticatedUsers());
				putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        widget.isVisibleToPublicUsers());
				putData(entry, WidgetTraitDefinition.SOURCE_PROPERTY,                              widget.getSource());
				putData(entry, WidgetTraitDefinition.DESCRIPTION_PROPERTY,                         widget.getDescription());
				putData(entry, WidgetTraitDefinition.IS_WIDGET_PROPERTY,                           widget.isWidget());
				putData(entry, WidgetTraitDefinition.TREE_PATH_PROPERTY,                           widget.getTreePath());
				putData(entry, WidgetTraitDefinition.CONFIGURATION_PROPERTY,                       widget.getConfiguration());
				putData(entry, WidgetTraitDefinition.IS_PAGE_TEMPLATE_PROPERTY,                    widget.isPageTemplate());
				putData(entry, WidgetTraitDefinition.SELECTORS_PROPERTY,                           widget.getSelectors());
			}

			tx.success();
		}

		writeJsonToFile(target, widgets);
	}

	private void exportApplicationConfigurationData(final Path target) throws FrameworkException {

		logger.info("Exporting application configuration data");

		final List<Map<String, Object>> applicationConfigurationDataNodes = new LinkedList<>();
		final Traits traits                                               = Traits.of(StructrTraits.APPLICATION_CONFIGURATION_DATA_NODE);
		final App app                                                     = StructrApp.getInstance();

		final PropertyKey<String> configTypeKey = traits.key(ApplicationConfigurationDataNodeTraitDefinition.CONFIG_TYPE_PROPERTY);

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.APPLICATION_CONFIGURATION_DATA_NODE).sort(configTypeKey).getAsList()) {

				final ApplicationConfigurationDataNode acdn = node.as(ApplicationConfigurationDataNode.class);
				final Map<String, Object> entry             = new TreeMap<>();

				applicationConfigurationDataNodes.add(entry);

				entry.put(GraphObjectTraitDefinition.ID_PROPERTY,                               acdn.getUuid());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                           acdn.getName());
				entry.put(ApplicationConfigurationDataNodeTraitDefinition.CONFIG_TYPE_PROPERTY, acdn.getConfigType());
				entry.put(ApplicationConfigurationDataNodeTraitDefinition.CONTENT_PROPERTY,     acdn.getContent());

				exportOwnershipAndSecurity(node, entry);
			}

			tx.success();
		}

		writeSortedCompactJsonToFile(target, applicationConfigurationDataNodes, new AbstractMapComparator<Object>() {
			@Override
			public String getKey (Map<String, Object> map) {

				final Object configType = map.get(ApplicationConfigurationDataNodeTraitDefinition.CONFIG_TYPE_PROPERTY);
				final Object name       = map.get(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				final Object id         = map.get(GraphObjectTraitDefinition.ID_PROPERTY);

				return (configType != null ? configType.toString() : "00-configType").concat((name != null ? name.toString() : "00-name")).concat(id.toString());
			}
		});
	}

	private void exportLocalizations(final Path target) throws FrameworkException {

		logger.info("Exporting localizations");

		final Traits traits                           = Traits.of(StructrTraits.LOCALIZATION);
		final List<Map<String, Object>> localizations = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.LOCALIZATION).sort(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>(new IdFirstComparator());
				final Localization localization = node.as(Localization.class);

				localizations.add(entry);

				entry.put(GraphObjectTraitDefinition.ID_PROPERTY,                             localization.getUuid());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                         localization.getName());
				entry.put(LocalizationTraitDefinition.LOCALIZED_NAME_PROPERTY,                localization.getLocalizedName());
				entry.put(LocalizationTraitDefinition.DOMAIN_PROPERTY,                        localization.getDomain());
				entry.put(LocalizationTraitDefinition.LOCALE_PROPERTY,                        localization.getLocale());
				entry.put(LocalizationTraitDefinition.IMPORTED_PROPERTY,                      localization.isImported());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, localization.isVisibleToAuthenticatedUsers());
				entry.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        localization.isVisibleToPublicUsers());
			}

			tx.success();
		}

		writeSortedCompactJsonToFile(target, localizations, new AbstractMapComparator<Object>() {

			@Override
			public String getKey (Map<String, Object> map) {

				final Object name   = map.get(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				final Object domain = map.get(LocalizationTraitDefinition.DOMAIN_PROPERTY);
				final Object locale = map.get(LocalizationTraitDefinition.LOCALE_PROPERTY);
				final Object id     = map.get(GraphObjectTraitDefinition.ID_PROPERTY);

				// null domain is replaced by a string so that those localizations are shown first
				return (name != null ? name.toString() : "null").concat((domain != null ? domain.toString() : "00-nulldomain")).concat((locale != null ? locale.toString() : "null")).concat(id.toString());
			}
		});
	}

	private boolean shouldExportActionMapping(final ActionMapping actionMapping) {

		final List<DOMElement> triggerElements = Iterables.toList(actionMapping.getTriggerElements()).stream().filter(domElement -> !domElement.inTrash()).toList();

		return (!triggerElements.isEmpty());
	}

	private void exportActionMapping(final Path target) throws FrameworkException {

		logger.info("Exporting action mapping");

		final List<Map<String, Object>> actionMappings    = new LinkedList<>();
		final App app                                     = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.ACTION_MAPPING).getAsList()) {

				final Map<String, Object> entry   = new TreeMap<>();
				final ActionMapping actionMapping = node.as(ActionMapping.class);

				if (shouldExportActionMapping(actionMapping)) {

					actionMappings.add(entry);

					putData(entry, GraphObjectTraitDefinition.ID_PROPERTY,                             actionMapping.getUuid());
					putData(entry, NodeInterfaceTraitDefinition.NAME_PROPERTY ,                        actionMapping.getName());
					putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, actionMapping.isVisibleToAuthenticatedUsers());
					putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        actionMapping.isVisibleToPublicUsers());

					putDataIfListNotEmpty(entry, ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY,              Iterables.toList(actionMapping.getTriggerElements()).stream().filter(domElement -> !domElement.inTrash()).map(GraphObject::getUuid).toList());
					putDataIfListNotEmpty(entry, ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY,               Iterables.toList(actionMapping.getSuccessTargets()).stream().map(GraphObject::getUuid).toList());
					putDataIfListNotEmpty(entry, ActionMappingTraitDefinition.SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY, Iterables.toList(actionMapping.getSuccessNotificationElements()).stream().map(GraphObject::getUuid).toList());
					putDataIfListNotEmpty(entry, ActionMappingTraitDefinition.FAILURE_TARGETS_PROPERTY,               Iterables.toList(actionMapping.getFailureTargets()).stream().map(GraphObject::getUuid).toList());
					putDataIfListNotEmpty(entry, ActionMappingTraitDefinition.FAILURE_NOTIFICATION_ELEMENTS_PROPERTY, Iterables.toList(actionMapping.getFailureNotificationElements()).stream().map(GraphObject::getUuid).toList());
					putDataIfListNotEmpty(entry, ActionMappingTraitDefinition.PARAMETER_MAPPINGS_PROPERTY,            Iterables.toList(actionMapping.getParameterMappings()).stream().map(GraphObject::getUuid).toList());

					putData(entry, ActionMappingTraitDefinition.EVENT_PROPERTY,                         actionMapping.getEvent());
					putData(entry, ActionMappingTraitDefinition.ACTION_PROPERTY,                        actionMapping.getAction());
					putData(entry, ActionMappingTraitDefinition.METHOD_PROPERTY,                        actionMapping.getMethod());
					putData(entry, ActionMappingTraitDefinition.DATA_TYPE_PROPERTY,                     actionMapping.getDataType());
					putData(entry, ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY,                 actionMapping.getIdExpression());

					putData(entry, ActionMappingTraitDefinition.DIALOG_TYPE_PROPERTY,                   actionMapping.getDialogType());
					putData(entry, ActionMappingTraitDefinition.DIALOG_TITLE_PROPERTY,                  actionMapping.getDialogTitle());
					putData(entry, ActionMappingTraitDefinition.DIALOG_TEXT_PROPERTY,                   actionMapping.getDialogText());

					putData(entry, ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY,             actionMapping.getSuccessBehaviour());
					putData(entry, ActionMappingTraitDefinition.SUCCESS_EVENT_PROPERTY,                 actionMapping.getSuccessEvent());
					putData(entry, ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY,         actionMapping.getSuccessNotifications());
					putData(entry, ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_EVENT_PROPERTY,   actionMapping.getSuccessNotificationsEvent());
					putData(entry, ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PARTIAL_PROPERTY, actionMapping.getSuccessNotificationsPartial());
					putData(entry, ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_DELAY_PROPERTY,   actionMapping.getSuccessNotificationsDelay());
					putData(entry, ActionMappingTraitDefinition.SUCCESS_PARTIAL_PROPERTY,               actionMapping.getSuccessPartial());
					putData(entry, ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY,                   actionMapping.getSuccessURL());

					putData(entry, ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY,             actionMapping.getFailureBehaviour());
					putData(entry, ActionMappingTraitDefinition.FAILURE_EVENT_PROPERTY,                 actionMapping.getFailureEvent());
					putData(entry, ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY,         actionMapping.getFailureNotifications());
					putData(entry, ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_EVENT_PROPERTY,   actionMapping.getFailureNotificationsEvent());
					putData(entry, ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PARTIAL_PROPERTY, actionMapping.getFailureNotificationsPartial());
					putData(entry, ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_DELAY_PROPERTY,   actionMapping.getFailureNotificationsDelay());
					putData(entry, ActionMappingTraitDefinition.FAILURE_PARTIAL_PROPERTY,               actionMapping.getFailurePartial());
					putData(entry, ActionMappingTraitDefinition.FAILURE_URL_PROPERTY,                   actionMapping.getFailureURL());
				}
			}

			tx.success();
		}

		writeJsonToFile(target, actionMappings);
	}

	private void exportParameterMapping(final Path target) throws FrameworkException {

		logger.info("Exporting parameter mapping");

		final List<Map<String, Object>> parameterMappings = new LinkedList<>();
		final Traits traits                               = Traits.of(StructrTraits.PARAMETER_MAPPING);
		final App app                                     = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery(StructrTraits.PARAMETER_MAPPING).sort(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				final ParameterMapping parameterMapping = node.as(ParameterMapping.class);
				final ActionMapping actionMapping       = parameterMapping.getActionMapping();

				if (actionMapping != null && shouldExportActionMapping(actionMapping)) {

					final Map<String, Object> entry = new TreeMap<>();
					parameterMappings.add(entry);

					putData(entry, GraphObjectTraitDefinition.ID_PROPERTY,                             parameterMapping.getUuid());
					putData(entry, NodeInterfaceTraitDefinition.NAME_PROPERTY ,                        parameterMapping.getName());
					putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, parameterMapping.isVisibleToAuthenticatedUsers());
					putData(entry, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        parameterMapping.isVisibleToPublicUsers());

					putData(entry, ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY,    parameterMapping.getParameterType());
					putData(entry, ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY,    parameterMapping.getParameterName());
					putData(entry, ParameterMappingTraitDefinition.CONSTANT_VALUE_PROPERTY,    parameterMapping.getConstantValue());
					putData(entry, ParameterMappingTraitDefinition.SCRIPT_EXPRESSION_PROPERTY, parameterMapping.getScriptExpression());

					DOMElement inputElement = parameterMapping.getInputElement();
					if (inputElement != null) {

						putData(entry, ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY, inputElement.getUuid());
					}
				}
			}

			tx.success();
		}

		writeJsonToFile(target, parameterMappings);
	}

	protected void putData(final Map<String, Object> target, final String key, final Object value) {

		if (value instanceof Iterable) {

			target.put(key, Iterables.toList((Iterable)value));

		} else {

			target.put(key, value);
		}
	}

	protected void putDataIfListNotEmpty(final Map<String, Object> target, final String key, final List list) {

		if (!list.isEmpty()) {

			target.put(key, list);
		}
	}

	protected List<Map<String, Object>> readConfigList(final Path conf) {

		try (final Reader reader = Files.newBufferedReader(conf, StandardCharsets.UTF_8)) {

			return getGson().fromJson(reader, List.class);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		return Collections.emptyList();
	}

	protected void applyConfigurationFileIfExists(final SecurityContext ctx, final Path confFile, final String progressType) {

		if (Files.exists(confFile)) {

			final App app = StructrApp.getInstance(ctx);

			try (final Tx tx = app.tx()) {

				tx.disableChangelog();

				String confSource = new String(Files.readAllBytes(confFile), StandardCharsets.UTF_8).trim();

				if (confSource.length() > 0) {

					if (confSource.startsWith("$")) {

						final String warnText = "Deployment config script '" + confFile + "' is using old syntax. This is now an auto-script environment, opening '${' and closing '}' are not necessary anymore. This is currently supported, but support for this old syntax will be removed in an upcoming version!";
						logger.warn(warnText);
						publishWarningMessage("Deprecation warning for " + confFile, warnText);

					} else {

						confSource = "${" + confSource + "}";
					}

					final String message = "Applying configuration from '" + confFile + "'";
					logger.info(message);
					publishProgressMessage(progressType, message);

					Scripting.evaluate(new ActionContext(ctx), null, confSource, confFile.getFileName().toString(), null);

				} else {

					logger.info("Ignoring empty configuration '{}'", confFile);
				}

				tx.success();

			} catch (Throwable t) {

				final String msg = "Exception caught while importing '" + confFile + "'";
				logger.warn(msg, t);
				publishWarningMessage(msg, t.toString());
			}
		}
	}

	private <T extends NodeInterface> void importListData(final String type, final List<Map<String, Object>> data, final PropertyMap... additionalData) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final NodeInterface toDelete : app.nodeQuery(type).getAsList()) {
				app.delete(toDelete);
			}

			for (final Map<String, Object> entry : data) {

				logger.debug("Importing {}", entry);

				checkOwnerAndSecurity(entry);

				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, type, entry);

				// allow caller to insert additional data for better creation performance
				for (final PropertyMap add : additionalData) {
					map.putAll(add);
				}

				app.create(type, map);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import {}, aborting with {}", type, fex.getMessage(), fex);

			throw fex;
		}
	}

	private void importSchemaGrants(final Path schemaGrantsMetadataFile) throws FrameworkException {

		if (Files.exists(schemaGrantsMetadataFile)) {

			logger.info("Reading {}", schemaGrantsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing schema permissions");

			importSchemaGrants(readConfigList(schemaGrantsMetadataFile));
		}
	}

	private void importSchemaGrants(final List<Map<String, Object>> data) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.SCHEMA_GRANT).getAsList()) {
				app.delete(toDelete);
			}

			for (final Map<String, Object> entry : data) {

				checkOwnerAndSecurity(entry);

				app.create(StructrTraits.SCHEMA_GRANT, PropertyMap.inputTypeToJavaType(context, StructrTraits.SCHEMA_GRANT, entry));
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import schema permission, aborting with {}", fex.getMessage(), fex);

			throw fex;
		}
	}

	private void importResourceAccessGrants(final Path grantsMetadataFile) throws FrameworkException {

		if (Files.exists(grantsMetadataFile)) {

			logger.info("Reading {}", grantsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing resource access permissions");

			importResourceAccessGrants(readConfigList(grantsMetadataFile));
		}
	}

	private void importResourceAccessGrants(final List<Map<String, Object>> data) throws FrameworkException {

		boolean isOldExport = false;
		final StringBuilder grantMessagesHtml = new StringBuilder();
		final StringBuilder grantMessagesText = new StringBuilder();

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.RESOURCE_ACCESS).getAsList()) {
				app.delete(toDelete);
			}

			for (final Map<String, Object> entry : data) {

				if (!entry.containsKey("grantees") && !entry.containsKey(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY) && !entry.containsKey(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)) {

					isOldExport = true;

					final long flags = ((Number)entry.get(ResourceAccessTraitDefinition.FLAGS_PROPERTY)).longValue();
					if (flags != 0) {

						final String signature = (String)entry.get(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY);

						final boolean hasAnyNonAuthFlags = ((flags & UiAuthenticator.NON_AUTH_USER_GET) == UiAuthenticator.NON_AUTH_USER_GET) ||
							((flags & UiAuthenticator.NON_AUTH_USER_PUT) == UiAuthenticator.NON_AUTH_USER_PUT) ||
							((flags & UiAuthenticator.NON_AUTH_USER_POST) == UiAuthenticator.NON_AUTH_USER_POST) ||
							((flags & UiAuthenticator.NON_AUTH_USER_DELETE) == UiAuthenticator.NON_AUTH_USER_DELETE) ||
							((flags & UiAuthenticator.NON_AUTH_USER_OPTIONS) == UiAuthenticator.NON_AUTH_USER_OPTIONS) ||
							((flags & UiAuthenticator.NON_AUTH_USER_HEAD) == UiAuthenticator.NON_AUTH_USER_HEAD) ||
							((flags & UiAuthenticator.NON_AUTH_USER_PATCH) == UiAuthenticator.NON_AUTH_USER_PATCH);

						final boolean hasAnyAuthFlags = ((flags & UiAuthenticator.AUTH_USER_GET) == UiAuthenticator.AUTH_USER_GET) ||
							((flags & UiAuthenticator.AUTH_USER_PUT) == UiAuthenticator.AUTH_USER_PUT) ||
							((flags & UiAuthenticator.AUTH_USER_POST) == UiAuthenticator.AUTH_USER_POST) ||
							((flags & UiAuthenticator.AUTH_USER_DELETE) == UiAuthenticator.AUTH_USER_DELETE) ||
							((flags & UiAuthenticator.AUTH_USER_OPTIONS) == UiAuthenticator.AUTH_USER_OPTIONS) ||
							((flags & UiAuthenticator.AUTH_USER_HEAD) == UiAuthenticator.AUTH_USER_HEAD) ||
							((flags & UiAuthenticator.AUTH_USER_PATCH) == UiAuthenticator.AUTH_USER_PATCH);

						if (hasAnyNonAuthFlags) {
							grantMessagesHtml.append("Signature <b>").append(signature).append("</b> was set to <code>visibleToPublicUsers: true</code><br>");
							grantMessagesText.append("    Signature '").append(signature).append("' was set to 'visibleToPublicUsers: true'\n");
						}

						if (hasAnyAuthFlags) {
							grantMessagesHtml.append("Signature <b>").append(signature).append("</b> was set to <code>visibleToAuthenticatedUsers: true</code><br>");
							grantMessagesText.append("    Signature '").append(signature).append("' was set to 'visibleToAuthenticatedUsers: true'\n");
						}

						if (hasAnyNonAuthFlags && hasAnyAuthFlags) {
							grantMessagesHtml.append("Signature <b>").append(signature).append("</b> is probably misconfigured and <b><u>should be split into two permissions</u></b>.<br>");
							grantMessagesText.append("    Signature '").append(signature).append("' is probably misconfigured and **should be split into two permissions**.\n");
						}

						entry.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, hasAnyAuthFlags);
						entry.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        hasAnyNonAuthFlags);
					}

				} else {
					checkOwnerAndSecurity(entry);
				}

				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, StructrTraits.RESOURCE_ACCESS, entry);

				app.create(StructrTraits.RESOURCE_ACCESS, map);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import resouce access permission, aborting with {}", fex.getMessage(), fex);

			throw fex;

		} finally {

			if (isOldExport) {

				final String text = "Found outdated version of grants.json file without visibility and grantees!\n\n"
						+ "    Configuration was auto-updated using this simple heuristic:\n"
						+ "     * Grants with public access were set to **visibleToPublicUsers: true**\n"
						+ "     * Grants with authenticated access were set to **visibleToAuthenticatedUsers: true**\n\n"
						+ "    Please make any necessary changes in the 'Security' area as this may not suffice for your use case. The ability to use group/user rights to permissions has been added to improve flexibility.";

				final String htmlText = "Configuration was auto-updated using this simple heuristic:<br>"
						+ "&nbsp;- Grants with public access were set to <code>visibleToPublicUsers: true</code><br>"
						+ "&nbsp;- Grants with authenticated access were set to <code>visibleToAuthenticatedUsers: true</code><br><br>"
						+ "Please make any necessary changes in the <a href=\"#security\">Security</a> area as this may not suffice for your use case. The ability to use group/user rights to permissions has been added to improve flexibility.";

				deferredLogTexts.add(text + "\n\n" + grantMessagesText);
				publishWarningMessage("Found grants.json file without visibility and grantees", htmlText + "<br><br>" + grantMessagesHtml);
			}
		}
	}

	private void importCorsSettings(final Path corsSettingsMetadataFile) throws FrameworkException {

		if (Files.exists(corsSettingsMetadataFile)) {

			logger.info("Reading {}", corsSettingsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing CORS Settings");

			importListData(StructrTraits.CORS_SETTING, readConfigList(corsSettingsMetadataFile));
		}
	}

	private void importMailTemplates(final Path mailTemplatesMetadataFile, final Path source) throws FrameworkException {

		if (Files.exists(mailTemplatesMetadataFile)) {

			logger.info("Reading {}", mailTemplatesMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing mail templates");

			final List<Map<String, Object>> mailTemplatesConf = readConfigList(mailTemplatesMetadataFile);

			final Path mailTemplatesFolder = source.resolve("mail-templates");

			if (Files.exists(mailTemplatesFolder)) {

				for (Map<String, Object> mailTpl : mailTemplatesConf) {

					final String filename = (String)mailTpl.remove("filename");
					final Path tplFile    = mailTemplatesFolder.resolve(filename);

					try {
						mailTpl.put("text", (Files.exists(tplFile)) ? new String(Files.readAllBytes(tplFile)) : null);
					} catch (IOException ioe) {
						logger.warn("Failed reading mail-tempalte file '{}'", filename);
					}
				}
			}

			importListData(StructrTraits.MAIL_TEMPLATE, mailTemplatesConf);
		}
	}

	private void importWidgets(final Path widgetsMetadataFile) throws FrameworkException {

		if (Files.exists(widgetsMetadataFile)) {

			logger.info("Reading {}", widgetsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing widgets");

			importListData(StructrTraits.WIDGET, readConfigList(widgetsMetadataFile));
		}
	}

	private void importLocalizations(final Path localizationsMetadataFile) throws FrameworkException {

		if (Files.exists(localizationsMetadataFile)) {

			final Traits traits              = Traits.of(StructrTraits.LOCALIZATION);
			final PropertyMap additionalData = new PropertyMap();

			// Question: shouldn't this be true?
			// No! 'imported' is a flag for legacy-localization which
			// have been imported from a legacy-system which was replaced by structr.
			// it is a way to differentiate between new and old localization strings
			additionalData.put(traits.key("imported"), false);

			logger.info("Reading {}", localizationsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing localizations");

			importListData(StructrTraits.LOCALIZATION, readConfigList(localizationsMetadataFile), additionalData);
		}
	}

	private void importApplicationConfigurationNodes(final Path applicationConfigurationDataMetadataFile) throws FrameworkException {

		if (Files.exists(applicationConfigurationDataMetadataFile)) {

			logger.info("Reading {}", applicationConfigurationDataMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing application configuration data");

			importListData(StructrTraits.APPLICATION_CONFIGURATION_DATA_NODE, readConfigList(applicationConfigurationDataMetadataFile));
		}
	}

	private FileImportVisitor.FileImportProblems importFiles(final Path filesMetadataFile, final Path source, final SecurityContext ctx) throws FrameworkException {

		if (Files.exists(filesMetadataFile)) {

			logger.info("Reading {}", filesMetadataFile);
			final Map<String, Object> filesMetadata = new HashMap<>(readMetadataFileIntoMap(filesMetadataFile));

			final Path files = source.resolve("files");
			if (Files.exists(files)) {

				final FileImportVisitor fiv = new FileImportVisitor(ctx, files, filesMetadata);

				try {

					logger.info("Importing files (unchanged files will be skipped)");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing files");

					Files.walkFileTree(files, fiv);

				} catch (IOException ioex) {

					logger.warn("Exception while importing files", ioex);
				}

				return fiv.getFileImportProblems();
			}
		}

		return null;
	}

	private void importModuleData(final Path source) throws FrameworkException {

		for (StructrModule module : StructrApp.getConfiguration().getModules().values()) {

			if (module.hasDeploymentData()) {

				final Path moduleFolder = source.resolve("modules/" + module.getName() + "/");

				if (Files.exists(moduleFolder)) {

					logger.info("Importing deployment data for module {}", module.getName());
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing deployment data for module " + module.getName());

					module.importDeploymentData(moduleFolder, getGson());
				}
			}
		}
	}

	private void importHTMLContent(final App app, final Path source, final Path pagesMetadataFile, final Path componentsMetadataFile, final Path templatesMetadataFile, final Path sitesConfFile, final Path pathsConfFile, final boolean extendExistingApp, final boolean relativeVisibility, final Map<DOMNode, PropertyMap> deferredNodesAndTheirProperties) throws FrameworkException {

		final Map<String, Object> componentsMetadata = new HashMap<>();
		final Map<String, Object> templatesMetadata  = new HashMap<>();
		final Map<String, Object> pagesMetadata      = new HashMap<>();

		// read pages.json
		if (Files.exists(pagesMetadataFile)) {

			logger.info("Reading {}", pagesMetadataFile);
			pagesMetadata.putAll(readMetadataFileIntoMap(pagesMetadataFile));
		}

		// read components.json
		if (Files.exists(componentsMetadataFile)) {

			logger.info("Reading {}", componentsMetadataFile);
			componentsMetadata.putAll(readMetadataFileIntoMap(componentsMetadataFile));
		}

		// read templates.json
		if (Files.exists(templatesMetadataFile)) {

			logger.info("Reading {}", templatesMetadataFile);
			templatesMetadata.putAll(readMetadataFileIntoMap(templatesMetadataFile));
		}

		final int keysTotal = componentsMetadata.size() + templatesMetadata.size() + pagesMetadata.size();
		if (keysTotal > 0) {

			// if no keys are defined we do not need to look at the respective directories

			// visibility check
			if (!relativeVisibility) {

				final String title = "Deprecation Notice";
				final String text = "The deployment export data currently being imported has been created with an older version of Structr (or deployment.conf is missing) "
						+ "in which the visibility flags of DOM elements were exported depending on the flags of the containing page (or deployment.conf is missing).\n\n"
						+ "***The data will be imported correctly, based on the old format.***\n\n"
						+ "After this import has finished, you should **export again to the same location** so that the deployment export data will be upgraded to the most recent format.";
				final String htmlText = "The deployment export currently being imported has been created with an older version of Structr "
						+ "in which the visibility flags of DOM elements were exported depending on the flags of the containing page (or deployment.conf is missing).<br><br>"
						+ "<b>The data will be imported correctly, based on the old format.</b><br><br>"
						+ "After this import has finished, you should <b>export again to the same location</b> so that the deployment export data will be upgraded to the most recent format.";

				deferredLogTexts.add(title + ": " + text);
				publishWarningMessage(title, htmlText);
			}

			// construct paths
			final Path templates  = source.resolve("templates");
			final Path components = source.resolve("components");
			final Path pages      = source.resolve("pages");

			// remove all DOMNodes from the database (clean webapp for import, but only
			// if the actual import directories exist, don't delete web components if
			// an empty directory was specified accidentially).
			if (!extendExistingApp && Files.exists(templates) && Files.exists(components) && Files.exists(pages)) {

				try (final Tx tx = app.tx()) {

					tx.disableChangelog();

					logger.info("Removing pages, templates and components");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing pages, templates and components");

					app.deleteAllNodesOfType(StructrTraits.DOM_NODE);

					if (Files.exists(sitesConfFile)) {

						logger.info("Removing sites");
						publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing sites");

						app.deleteAllNodesOfType(StructrTraits.SITE);
					}

					FlushCachesCommand.flushAll();

					tx.success();
				}

			} else {

				logger.info("Import directory does not seem to contain pages, templates or components, NOT removing any data.");
			}

			// import templates, must be done before pages so the templates exist
			if (Files.exists(templates)) {

				try {

					logger.info("Importing templates");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing templates");

					final TemplateImporter visitor = new TemplateImporter(templatesMetadata);
					visitor.processFolderContentsSorted(templates);

				} catch (IOException ioex) {
					logger.warn("Exception while importing templates", ioex);
				}
			}

			// make sure shadow document is created in any case
			CreateComponentCommand.getOrCreateHiddenDocument();

			// import components, must be done before pages so the shared components exist
			if (Files.exists(components)) {

				try {

					logger.info("Importing shared components");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing shared components");

					final ComponentImporter visitor = new ComponentImporter(componentsMetadata, relativeVisibility);
					visitor.setDeferredNodesAndTheirProperties(deferredNodesAndTheirProperties);

					// first, only import the HULL for each shared component (the outermost element)
					visitor.setHullMode(true);
					visitor.processFolderContentsSorted(components);

					// then, when all of those are imported, import their children so we know they all exist
					visitor.setHullMode(false);
					visitor.processFolderContentsSorted(components);

				} catch (IOException ioex) {
					logger.warn("Exception while importing shared components", ioex);
				}
			}

			// import pages
			if (Files.exists(pages)) {

				try {

					logger.info("Importing pages");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing pages");

					final PageImporter visitor = new PageImporter(pages, pagesMetadata, relativeVisibility);
					visitor.setDeferredNodesAndTheirProperties(deferredNodesAndTheirProperties);

					visitor.processFolderContentsSorted(pages);

				} catch (IOException ioex) {
					logger.warn("Exception while importing pages", ioex);
				}
			}
		}

		handleDeferredProperties();

		if (Files.exists(sitesConfFile)) {

			logger.info("Importing sites");
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing sites");

			importSites(readConfigList(sitesConfFile));
		}

		if (Files.exists(pathsConfFile)) {

			logger.info("Importing page paths");
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing page paths");

			try (final Tx tx = app.tx()) {

				tx.disableChangelog();

				// remove existing paths
				for (final NodeInterface path : app.nodeQuery(StructrTraits.PAGE_PATH).getResultStream()) {
					app.delete(path);
				}

				importPagePaths(readConfigList(pathsConfFile));

				tx.success();
			}
		}
	}

	private void importActionMapping(final Path path) throws FrameworkException {

		if (Files.exists(path)) {

			logger.info("Reading {}", path);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing action mapping");

			importListData(StructrTraits.ACTION_MAPPING, readConfigList(path));
		}
	}

	private void importParameterMapping(final Path parameterMappingPath) throws FrameworkException {

		if (Files.exists(parameterMappingPath)) {

			logger.info("Reading {}", parameterMappingPath);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing parameter mapping");

			importListData(StructrTraits.PARAMETER_MAPPING, readConfigList(parameterMappingPath));
		}
	}

	private void handleDeferredProperties() throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		final App app                 = StructrApp.getInstance(context);

		context.setDoTransactionNotifications(false);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final DOMNode node : deferredNodesAndTheirProperties.keySet()) {

				final PropertyMap properties = deferredNodesAndTheirProperties.get(node);

				for (final PropertyKey propertyKey : properties.keySet()) {

					final PropertyConverter inputConverter = propertyKey.inputConverter(securityContext, true);

					if (inputConverter != null) {

						node.setProperty(propertyKey, inputConverter.convert(properties.get(propertyKey)));
					}
				}

			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to handle deferred properties, aborting with {}", fex.getMessage(), fex);

			throw fex;
		}
	}

	private void importSites(final List<Map<String, Object>> data) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		final Traits traits           = Traits.of(StructrTraits.SITE);
		final App app                 = StructrApp.getInstance(context);

		context.setDoTransactionNotifications(false);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (Map<String, Object> entry : data) {

				final List<NodeInterface> pages = new LinkedList();

				for (final String pageName : (List<String>)entry.get(SiteTraitDefinition.PAGES_PROPERTY)) {
					pages.add(app.nodeQuery(StructrTraits.PAGE).name(pageName).getFirst());
				}

				entry.remove(SiteTraitDefinition.PAGES_PROPERTY);

				checkOwnerAndSecurity(entry);

				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, StructrTraits.SITE, entry);

				map.put(traits.key(SiteTraitDefinition.PAGES_PROPERTY), pages);

				app.create(StructrTraits.SITE, map);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import site, aborting with {}", fex.getMessage(), fex);

			throw fex;
		}
	}

	private void importPagePaths(final List<Map<String, Object>> data) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		final App app                 = StructrApp.getInstance(context);

		context.setDoTransactionNotifications(false);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (Map<String, Object> entry : data) {

				final List<Map<String, Object>> parameterEntriesInput = (List) entry.get(PagePathTraitDefinition.PARAMETERS_PROPERTY);
				final List<NodeInterface> parameters                  = new LinkedList<>();

				for (final Map<String, Object> parameterEntry : parameterEntriesInput) {

					// create parameter nodes
					parameters.add(app.create(StructrTraits.PAGE_PATH_PARAMETER, PropertyMap.inputTypeToJavaType(context, StructrTraits.PAGE_PATH_PARAMETER, parameterEntry)));
				}

				// remove from root entry to prevent evaluation of parameters in inputTypeToJavaType below
				entry.remove(PagePathTraitDefinition.PARAMETERS_PROPERTY);

				// create path node
				final NodeInterface path = app.create(StructrTraits.PAGE_PATH, PropertyMap.inputTypeToJavaType(context, StructrTraits.PAGE_PATH, entry));

				// store imported page path parameters in path node
				path.setProperty(path.getTraits().key(PagePathTraitDefinition.PARAMETERS_PROPERTY), parameters);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import page path, aborting with {}", fex.getMessage(), fex);

			throw fex;
		}
	}

	private void linkDeferredPages(final App app) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.PAGE);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			deferredPageLinks.forEach((String linkableUUID, String pagePath) -> {

				try {

					final NodeInterface linkElementNode = StructrApp.getInstance().getNodeById(StructrTraits.DOM_NODE, linkableUUID);
					final NodeInterface linkedPageNode  = StructrApp.getInstance().nodeQuery(StructrTraits.LINKABLE)
							.or()
							.key(traits.key(DOMElementTraitDefinition.PATH_PROPERTY), pagePath)
							.key(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), pagePath)
							.getFirst();

					final LinkSource linkSource = linkElementNode.as(LinkSource.class);
					final Linkable linkable     = linkedPageNode.as(Linkable.class);

					linkSource.setLinkable(linkable);

				} catch (Throwable t) {

					t.printStackTrace();
				}
			});

			deferredPageLinks.clear();

			tx.success();
		}
	}

	private void importEmbeddedApplicationData(final Path source) {

		final Path dataDir = source.resolve("data");
		if (Files.exists(dataDir) && Files.isDirectory(dataDir)) {

			logger.info("Importing application data");
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing application data");

			final DeployDataCommand cmd = StructrApp.getInstance(securityContext).command(DeployDataCommand.class);

			cmd.doImportFromDirectory(dataDir);
		}
	}

	private void importSchema(final Path schemaFolder, final boolean extendExistingSchema) throws FrameworkException {

		if (Files.exists(schemaFolder)) {

			try {

				logger.info("Importing data from schema/ directory");
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing schema");

				final Path schemaJsonFile = schemaFolder.resolve("schema.json");

				if (!Files.exists(schemaJsonFile)) {

					logger.info("Deployment does not contain schema/schema.json - continuing without schema import");

				} else {

					final SecurityContext ctx = SecurityContext.getSuperUserInstance();
					ctx.setDoTransactionNotifications(false);

					final App app = StructrApp.getInstance(ctx);

					try (final FileReader reader = new FileReader(schemaJsonFile.toFile())) {

						final StructrSchemaDefinition schema   = (StructrSchemaDefinition)StructrSchema.createFromSource(reader);
						final boolean shouldLoadSourceFromFile = schema.hasMethodSourceCodeInFiles();

						// The following block takes the relative file name in the source property of a schema method
						// and loads the actual source code from a file on disk.

						if (shouldLoadSourceFromFile) {

							final Path globalMethodsFolder = schemaFolder.resolve(DEPLOYMENT_SCHEMA_GLOBAL_METHODS_FOLDER);

							if (Files.exists(globalMethodsFolder)) {

								for (Map<String, Object> schemaMethod : schema.getUserDefinedFunctions()) {

									final String methodName = (String) schemaMethod.get("name");

									final Path globalMethodSourceFile = globalMethodsFolder.resolve(methodName);
									schemaMethod.put(DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY, (Files.exists(globalMethodSourceFile)) ? new String(Files.readAllBytes(globalMethodSourceFile)) : null);
								}
							}

							for (final StructrTypeDefinition typeDef : schema.getTypeDefinitions()) {

								final Path typeFolder = schemaFolder.resolve(typeDef.getName());

								if (Files.exists(typeFolder)) {

									final Path functionsFolder = typeFolder.resolve(DEPLOYMENT_SCHEMA_FUNCTIONS_FOLDER);

									if (Files.exists(functionsFolder)) {

										for (final Object propDef : typeDef.getProperties()) {

											if (propDef instanceof StructrFunctionProperty fp) {

												if (fp.getReadFunction() != null) {

													final Path readFunctionSourceFile = functionsFolder.resolve(fp.getName() + DEPLOYMENT_SCHEMA_READ_FUNCTION_SUFFIX);

													if (Files.exists(readFunctionSourceFile)) {
														fp.setReadFunction(new String(Files.readAllBytes(readFunctionSourceFile)));
													} else {
														fp.setReadFunction(null);
														DeployCommand.addMissingSchemaFile(schemaFolder.relativize(readFunctionSourceFile).toString());
													}
												}

												if (fp.getWriteFunction() != null) {

													final Path writeFunctionSourceFile = functionsFolder.resolve(fp.getName() + DEPLOYMENT_SCHEMA_WRITE_FUNCTION_SUFFIX);

													if (Files.exists(writeFunctionSourceFile)) {
														fp.setWriteFunction(new String(Files.readAllBytes(writeFunctionSourceFile)));
													} else {
														fp.setWriteFunction(null);
														DeployCommand.addMissingSchemaFile(schemaFolder.relativize(writeFunctionSourceFile).toString());
													}
												}
											}
										}
									}

									final Path methodsFolder = typeFolder.resolve(DEPLOYMENT_SCHEMA_METHODS_FOLDER);

									if (Files.exists(methodsFolder)) {

										for (final Object m : typeDef.getMethods()) {

											final StructrMethodDefinition method = (StructrMethodDefinition)m;
											final String uniqueMethodName        = method.getUniqueName();

											if (method.getSource() != null) {

												// unique method name does not include number of parameters any more!
												Path methodSourceFile = methodsFolder.resolve(uniqueMethodName);
												if (!Files.exists(methodSourceFile)) {

													// deprecated export name includes the number of parameters, check this one as well
													methodSourceFile = methodsFolder.resolve(uniqueMethodName + "." + method.getParameters().size());
												}

												if (Files.exists(methodSourceFile)) {

													method.setSource(new String(Files.readAllBytes(methodSourceFile)));

												} else {

													// fallback to allow import of exports with the unsuffixed method name
													final Path methodSourceFileFallback = methodsFolder.resolve(method.getName());
													if (Files.exists(methodSourceFileFallback)) {

														method.setSource(new String(Files.readAllBytes(methodSourceFileFallback)));

													} else {

														method.setSource(null);
														DeployCommand.addMissingSchemaFile(schemaFolder.relativize(methodSourceFile).toString());
													}
												}
											}
										}
									}
								}
							}
						}

						if (extendExistingSchema) {

							StructrSchema.extendDatabaseSchema(app, schema);

						} else {

							StructrSchema.replaceDatabaseSchema(app, schema);
						}

					} catch (Throwable t) {

						throw new ImportFailureException(t.getMessage(), t);
					}
				}

			} catch (ImportFailureException fex) {

				logger.warn("Unable to import schema: {}", fex.getMessage());
				if (fex.getCause() instanceof FrameworkException) {
					logger.warn("Caused by: {}", fex.getCause().toString());
				}
				throw new FrameworkException(422, fex.getMessage(), fex.getErrorBuffer());

			} catch (Throwable t) {
				logger.warn("Unable to import schema: {}", t.getMessage());
			}
		}
	}

	private boolean isDOMNodeVisibilityRelativeToParent(final Map<String, String> deploymentConf) {
		return DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_PARENT_VALUE.equals(deploymentConf.get(DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_KEY));
	}

	protected Map<String, String> readDeploymentConfigurationFile (final Path confFile) {

		final Map<String, String> deploymentConf = new LinkedHashMap<>();

		if (Files.exists(confFile)) {

			try {

				final FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
						.configure(new Parameters().properties()
								.setFile(confFile.toFile())
								.setThrowExceptionOnMissing(true)
								.setListDelimiterHandler(new DefaultListDelimiterHandler('\0'))
								.setIncludesAllowed(false)
						);

				final PropertiesConfiguration config = builder.getConfiguration();

				final Iterator<String> keys          = config.getKeys();

				while (keys.hasNext()) {

					final String key   = keys.next();
					final String value = StringUtils.trim(config.getString(key));

					deploymentConf.put(key, value);
				}

			} catch (Throwable t) {

				final String msg = "Exception caught while importing '" + confFile + "'";
				logger.warn(msg, t);
				publishWarningMessage(msg, t.toString());
			}
		}

		return deploymentConf;
	}

	protected void writeDeploymentConfigurationFile (final Path confFile) {

		try {

			final String message = "Writing deployment config file '" + confFile + "'";
			logger.info(message);
			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, message);

			final FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
					.configure(new Parameters().properties()
							.setFile(confFile.toFile())
							.setThrowExceptionOnMissing(true)
							.setListDelimiterHandler(new DefaultListDelimiterHandler('\0'))
							.setIncludesAllowed(false)
					);


			// Touch file, if it doesn't exist
			confFile.toFile().createNewFile();

			final PropertiesConfiguration config = builder.getConfiguration();

			config.setProperty(DEPLOYMENT_VERSION_KEY,                         VersionHelper.getFullVersionInfo());
			config.setProperty(DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_KEY, DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_PARENT_VALUE);
			config.setProperty(DEPLOYMENT_UUID_FORMAT_KEY,                     Settings.UUIDv4AllowedFormats.getValue());

			final FileHandler fileHandler = builder.getFileHandler();
			fileHandler.save();

		} catch (Throwable t) {

			final String msg = "Exception caught while importing '" + confFile + "'";
			logger.warn(msg, t);
			publishWarningMessage(msg, t.toString());
		}
	}

	void deleteDirectoryContentsRecursively (final Path path) throws IOException {

		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {

			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {

				for (Path entry : entries) {
					deleteRecursively(entry);
				}
			}
		}
	}

	public void deleteRecursively(final Path path) throws IOException {

		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {

			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {

				for (Path entry : entries) {
					deleteRecursively(entry);
				}
			}
		}

		Files.delete(path);
	}

	protected void writeStringToFile(final Path path, final String string) {

		try (final Writer writer = new FileWriter(path.toFile())) {

			if (string != null) {
				writer.write(string);
			}
			writer.flush();
			writer.close();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	protected void writeJsonToFile(final Path path, final Object data) {

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(path.toFile()))) {

			getGson().toJson(data, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	protected void writeSortedCompactJsonToFile (final Path target, final List<Map<String, Object>> objects, final AbstractMapComparator<Object> sortComparator) {

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			if (sortComparator != null) {
				objects.sort(sortComparator);
			}

			final Gson gson = new GsonBuilder().serializeNulls().create();

			final StringBuilder sb = new StringBuilder("[");

			List<String> jsonStrings = new LinkedList();

			for (Map<String, Object> obj : objects) {
				jsonStrings.add("\t" + gson.toJson(obj));
			}

			if (!jsonStrings.isEmpty()) {
				sb.append("\n").append(String.join(",\n", jsonStrings)).append("\n");
			}

			sb.append("]");

			fos.write(sb.toString());

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private boolean isTrue(final Object value) {

		if (value != null) {

			return "true".equalsIgnoreCase(value.toString());
		}

		return false;
	}

	private void checkDeploymentExportVersionIsCompatible(final Map<String, String> deploymentConf) throws ImportPreconditionFailedException {

		// version check (don't import deployment exports from newer versions!)
		if (!acceptDeploymentExportVersion(deploymentConf)) {

			final String currentVersion = VersionHelper.getFullVersionInfo();
			final String exportVersion  = StringUtils.defaultIfEmpty(deploymentConf.get(DEPLOYMENT_VERSION_KEY), "pre 3.5");

			final String title = "Incompatible Deployment Import";
			final String text = "The deployment export data currently being imported has been created with a newer version of Structr "
					+ "which is not supported because of incompatible changes in the deployment format.\n"
					+ "Current version: " + currentVersion + "\n"
					+ "Export version:  " + exportVersion;

			final String htmlText = "The deployment export data currently being imported has been created with a newer version of Structr "
					+ "which is not supported because of incompatible changes in the deployment format.<br><br><table>"
					+ "<tr><td class=\"font-bold pr-2\">Current version:</td><td>" + currentVersion + "</td></tr>"
					+ "<tr><td class=\"font-bold pr-2\">Export version:</td><td>" + exportVersion + "</td></tr>"
					+ "</table>";

			throw new ImportPreconditionFailedException(title, text, htmlText);
		}
	}

	private boolean acceptDeploymentExportVersion(final Map<String, String> deploymentConf) {

		final int currentVersion = parseVersionString(VersionHelper.getFullVersionInfo());
		final int exportVersion  = parseVersionString(deploymentConf.get(DEPLOYMENT_VERSION_KEY));

		return currentVersion >= exportVersion;
	}

	private void checkDeploymentExportUUIDFormatIsCompatible(final Map<String, String> deploymentConfig) throws ImportPreconditionFailedException {

		final String uuidFormatInDeployment = deploymentConfig.get(DEPLOYMENT_UUID_FORMAT_KEY);
		final String ourUUIDFormat          = Settings.UUIDv4AllowedFormats.getValue();

		// allow importing older exports without the entry
		if (uuidFormatInDeployment == null) {

			final String message     = "Deployment configuration does not contain information about the UUIDv4 format. If you know the deployment data to be originating from an identically configured instance you can safely ignore this message. With the next export, this setting will be written to the export folder. " +
					"Otherwise, make sure that your current configuration '" + Settings.UUIDv4AllowedFormats.getKey() + "=" + ourUUIDFormat + "' is compatible with the UUIDv4 format of the export data! " +
					"Continuing with import - if there are any problems, check the UUIDv4 format setting against the data in the export and configure this instance accordingly. If the formats differ, it might be advisable to start with a fresh database.";
			final String htmlMessage = "Deployment configuration does not contain information about the UUIDv4 format. If you know the deployment data to be originating from an identically configured instance you can safely ignore this message. With the next export, this setting will be written to the export folder.<br><br>" +
					"Otherwise, make sure that your current configuration '<b>" + Settings.UUIDv4AllowedFormats.getKey() + "=" + ourUUIDFormat + "</b>' is compatible with the UUIDv4 format of the export data!<br><br>" +
					"Continuing with import - if there are any problems, check the UUIDv4 format setting against the data in the export and configure this instance accordingly. If the formats differ, it might be advisable to start with a fresh database.";

			logger.info(message);

			publishInfoMessage("UUIDv4 format of export unknown", htmlMessage);

		} else if (!ourUUIDFormat.equals(uuidFormatInDeployment)) {

			if (Settings.POSSIBLE_UUID_V4_FORMATS.both.toString().equals(ourUUIDFormat)) {

				final String message     = "The export data is configured as having UUIDv4 format '" + uuidFormatInDeployment + "'. This instance is configured to accept both supported kinds of UUIDv4 formats. This should only ever be a temporary state to consolidate nodes to a single UUIDv4 format. Keeping this configuration permanently is neither encouraged nor supported.";
				final String htmlMessage = "The export data is configured as having UUIDv4 format '<b>" + uuidFormatInDeployment + "</b>'. This instance is configured to accept <b>both</b> supported kinds of UUIDv4 formats.<br><br>This should only ever be a temporary state to consolidate nodes to a single UUIDv4 format. Keeping this configuration permanently is neither encouraged nor supported.";

				// the current instance can handle both - allow import but complain
				logger.warn(message);

				publishWarningMessage("UUIDv4 format setting '" + ourUUIDFormat + "' active", htmlMessage);

			} else {

				final String title = "Incompatible Deployment Import";
				final String text = "The deployment export data currently being imported uses a different UUIDv4 format than this instance has configured. This makes the data incompatible. Please re-configure the instance to allow for the UUIDv4 format in the export.\n"
						+ "If there is already data in this instance, the UUIDv4 format of those nodes should be updated to reflect the export data (or vice versa)."
						+ "Export UUIDv4 format:  " + uuidFormatInDeployment + "\n"
						+ "Configured UUIDv4 format: " + ourUUIDFormat;

				final String htmlText = "The deployment export data currently being imported uses a different UUIDv4 format than this instance has configured. This makes the data incompatible. Please re-configure the instance to allow for the UUIDv4 format in the export.<br><br>"
						+ "If there is already data in this instance, the UUIDv4 format of those nodes should be updated to reflect the export data (or vice versa).<br><br><table>"
						+ "<tr><td class=\"font-bold pr-2\">Export UUIDv4 format:</td><td>" + uuidFormatInDeployment + "</td></tr>"
						+ "<tr><td class=\"font-bold pr-2\">Configured UUIDv4 format:</td><td>" + ourUUIDFormat + "</td></tr>"
						+ "</table>";

				throw new ImportPreconditionFailedException(title, text, htmlText);
			}
		}
	}

	private int parseVersionString(final String source) {

		if (source != null) {

			// "normalize" version string by removing all non-digit
			// characters and take only the first two digits
			final String[] parts = source.split(" ");
			final String digits  = parts[0].replaceAll("[\\D]*", "");

			try {

				return Integer.valueOf(digits.substring(0, 2));

			} catch (Throwable t) {}
		}

		// no version info present => return 0
		return 0;
	}

	@Override
	public int getCommandStatusCode() {
		return statusCode;
	}

	public void setCommandStatusCode(final int status) {
		statusCode = status;
	}

	@Override
	public Object getCommandResult() {

		if (customResult != null) {
			return customResult;
		}

		return Collections.EMPTY_LIST;
	}

	public void setCustomCommandResult(final Object result) {
		customResult = result;
	}

	protected String transformCountedMapToHumanReadableList(final Map<String, Integer> map, final String separator) {

		return map.entrySet()
				.stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.map(entry -> entry.getValue() + "x " + entry.getKey())
				.collect(Collectors.joining(separator));
	}

	// ----- public static methods -----
	public static void addDeferredPagelink (String linkableUUID, String pagePath) {
		deferredPageLinks.put(linkableUUID, pagePath);
	}

	public static void updateDeferredPagelink (String initialUUID, String correctUUID) {

		if (deferredPageLinks.containsKey(initialUUID) && !initialUUID.equals(correctUUID)) {
			deferredPageLinks.put(correctUUID, deferredPageLinks.get(initialUUID));
			deferredPageLinks.remove(initialUUID);
		}
	}

	public static void encounteredMissingPrincipal(final String errorPrefix, final String principalName) {

		if (!missingPrincipals.containsKey(principalName)) {

			logger.warn("{}! No node of type Principal with name '{}' found, ignoring.", errorPrefix, principalName);
			missingPrincipals.put(principalName, 1);

		} else {

			missingPrincipals.put(principalName, missingPrincipals.get(principalName) + 1);
		}
	}

	public static void encounteredAmbiguousPrincipal(final String errorPrefix, final String principalName, final int numberOfHits) {

		if (!ambiguousPrincipals.containsKey(principalName)) {

			logger.warn("{}! Found {} nodes of type Principal named '{}', ignoring.\"", errorPrefix, numberOfHits, principalName);
			ambiguousPrincipals.put(principalName, 1);

		} else {

			ambiguousPrincipals.put(principalName, ambiguousPrincipals.get(principalName) + 1);
		}
	}

	public static void addMissingSchemaFile (final String fileName) {
		missingSchemaFile.add(fileName);
	}

	// ----- nested helper classes -----
	protected static class IdFirstComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			if (o1 != null && o1.equals(o2)) {
				return 0;
			}
			if ("id".equals(o1)) {
				return -1;
			}
			if ("id".equals(o2)) {
				return 1;
			}
			return o1.compareTo(o2);
		}
	}

	// File Visitor to set group ownership on files created by the deployment export.
	public static class GroupAddFileVisitor extends SimpleFileVisitor<Path> {

		GroupPrincipal group;

		GroupAddFileVisitor(GroupPrincipal group) {
			super();
			this.group = group;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {

			try {

				final PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

				if (view != null) {

					view.setGroup(this.group);

					final Set<PosixFilePermission> newPerms = Files.getPosixFilePermissions(file, LinkOption.NOFOLLOW_LINKS);
					newPerms.add(PosixFilePermission.GROUP_READ);
					newPerms.add(PosixFilePermission.GROUP_WRITE);

					Files.setPosixFilePermissions(file, newPerms);
				}

			} catch (IOException e) {

				throw new RuntimeException(e);
			}

			return CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {

			try {

				final PosixFileAttributeView view = Files.getFileAttributeView(dir, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

				if (view != null) {

					view.setGroup(this.group);

					final Set<PosixFilePermission> newPerms = Files.getPosixFilePermissions(dir, LinkOption.NOFOLLOW_LINKS);
					newPerms.add(PosixFilePermission.GROUP_READ);
					newPerms.add(PosixFilePermission.GROUP_WRITE);
					newPerms.add(PosixFilePermission.GROUP_EXECUTE);

					Files.setPosixFilePermissions(dir, newPerms);
				}

			} catch (IOException e) {

				throw new RuntimeException(e);
			}

			return CONTINUE;
		}
	}
}
