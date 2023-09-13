/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.VersionHelper;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.StaticValue;
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
import org.structr.module.StructrModule;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.JavaScriptSource;
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
import org.structr.web.maintenance.deploy.*;
import org.structr.websocket.command.CreateComponentCommand;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitResult.CONTINUE;

public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger                     = LoggerFactory.getLogger(DeployCommand.class.getName());
	private static final Pattern pattern                   = Pattern.compile("[a-f0-9]{32}");

	private static final Map<String, String> deferredPageLinks        = new LinkedHashMap<>();
	private Map<DOMNode, PropertyMap> deferredNodesAndTheirProperties = new LinkedHashMap<>();

	protected static final Set<String> missingPrincipals       = new HashSet<>();
	protected static final Set<String> missingSchemaFile       = new HashSet<>();
	protected static final Set<String> deferredLogTexts        = new HashSet<>();


	protected static final AtomicBoolean deploymentActive      = new AtomicBoolean(false);

	private final static String DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_KEY          = "visibility-flags-relative-to";
	private final static String DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_PARENT_VALUE = "parent";
	private final static String DEPLOYMENT_VERSION_KEY                                  = "structr-version";
	private final static String DEPLOYMENT_DATE_KEY                                     = "deployment-date";

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
	private final static String GRANTS_FILE_PATH                                      = "security/grants.json";
	private final static String CORS_SETTINGS_FILE_PATH                               = "security/cors-settings.json";
	private final static String MAIL_TEMPLATES_FILE_PATH                              = "mail-templates.json";
	private final static String WIDGETS_FILE_PATH                                     = "widgets.json";
	private final static String LOCALIZATIONS_FILE_PATH                               = "localizations.json";
	private final static String APPLICATION_CONFIGURATION_DATA_FILE_PATH              = "application-configuration-data.json";
	private final static String FILES_FILE_PATH                                       = "files.json";
	private final static String PAGES_FILE_PATH                                       = "pages.json";
	private final static String COMPONENTS_FILE_PATH                                  = "components.json";
	private final static String TEMPLATES_FILE_PATH                                   = "templates.json";
	private final static String ACTION_MAPPING_FILE_PATH                              = "events/action-mapping.json";
	private final static String PARAMETER_MAPPING_FILE_PATH                           = "events/parameter-mapping.json";
	private final static String SITES_FILE_PATH                                       = "sites.json";
	private final static String SCHEMA_FOLDER_PATH                                    = "schema";
	private final static String COMPONENTS_FOLDER_PATH                                = "components";
	private final static String FILES_FOLDER_PATH                                     = "files";
	private final static String PAGES_FOLDER_PATH                                     = "pages";
	private final static String SECURITY_FOLDER_PATH                                  = "security";
	private final static String TEMPLATES_FOLDER_PATH                                 = "templates";
	private final static String EVENTS_FOLDER_PATH                                    = "events";
	private final static String MODULES_FOLDER_PATH                                   = "modules";
	private final static String MAIL_TEMPLATES_FOLDER_PATH                            = "mail-templates";

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("deploy", DeployCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> parameters) throws FrameworkException {

		final String mode = (String) parameters.get("mode");

		if (Boolean.FALSE.equals(deploymentActive.get())) {

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

			try (final Reader reader = Files.newBufferedReader(metadataFile, Charset.forName("utf-8"))) {

				return new HashMap<>(getGson().fromJson(reader, Map.class));

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}

		return new HashMap<>();
	}

	public StreamingJsonWriter getJsonWriter() {
		return new StreamingJsonWriter(new StaticValue<String>(PropertyView.All), true, 1, false, true);
	}

	public Gson getGson() {
		return new GsonBuilder().setPrettyPrinting().setDateFormat(Settings.DefaultDateFormat.getValue()).serializeNulls().create();
	}

	public static boolean isUuid(final String name) {
		return pattern.matcher(name).matches();
	}

	/**
	 * Checks if the given string ends with a uuid.
	 */
	public static boolean endsWithUuid(final String name) {
		if (name.length() > 32) {

			return pattern.matcher(name.substring(name.length() - 32)).matches();

		} else {

			return false;
		}
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

			if (source.isAbsolute() != true) {

				throw new ImportPreconditionFailedException("Source path '" + path + "' is not an absolute path - relative paths are not allowed.");
			}

			// Define all files/folders beforehand
			final Path deploymentConfFile                       = source.resolve(DEPLOYMENT_CONF_FILE_PATH);
			final Path preDeployConfFile                        = source.resolve(PRE_DEPLOY_CONF_FILE_PATH);
			final Path postDeployConfFile                       = source.resolve(POST_DEPLOY_CONF_FILE_PATH);
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
				!Files.exists(schemaFolder)
			) {

				throw new ImportPreconditionFailedException("Source path '" + path + "' does not contain any of the files for a structr deployment.");
			}

			logger.info("Importing from '{}'", path);

			// read deployment.conf (file containing information about deployment export)
			final Map<String, String> deploymentConf = readDeploymentConfigurationFile(deploymentConfFile);
			final boolean relativeVisibility         = isDOMNodeVisibilityRelativeToParent(deploymentConf);

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
						+ "<tr><th>Current version: </th><td>" + currentVersion + "</td></tr>"
						+ "<tr><th>Export version: </th><td>" + exportVersion + "</td></tr>"
						+ "</table>";

				logger.info(title + ": " + text);
				publishWarningMessage(title, htmlText);

				return;
			}

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

			importFiles(filesMetadataFile, source, ctx);

			importModuleData(source);

			importHTMLContent(app, source, pagesMetadataFile, componentsMetadataFile, templatesMetadataFile, sitesConfFile, extendExistingApp, relativeVisibility, deferredNodesAndTheirProperties);

			linkDeferredPages(app);

			importParameterMapping(parameterMappingMetadataFile);

			importActionMapping(actionMappingMetadataFile);

			importEmbeddedApplicationData(source);


			// apply post-deploy.conf
			applyConfigurationFileIfExists(ctx, postDeployConfFile, DEPLOYMENT_IMPORT_STATUS);

			if (!missingPrincipals.isEmpty()) {

				final String title = "Missing Principal(s)";
				final String text = "The following user(s) and/or group(s) are missing for grants or node ownership during <b>deployment</b>.<br>"
						+ "Because of these missing grants/ownerships, <b>the functionality is not identical to the export you just imported</b>!"
						+ "<ul><li>" + String.join("</li><li>",  missingPrincipals) + "</li></ul>"
						+ "Consider adding these principals to your <a href=\"https://docs.structr.com/docs/fundamental-concepts#pre-deployconf\">pre-deploy.conf</a> and re-importing.";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tThe following user(s) and/or group(s) are missing for grants or node ownership during deployment.\n"
						+ "\tBecause of these missing grants/ownerships, the functionality is not identical to the export you just imported!\n\n"
						+ "\t" + String.join("\n\t",  missingPrincipals)
						+ "\n\n\tConsider adding these principals to your 'pre-deploy.conf' (see https://docs.structr.com/docs/fundamental-concepts#pre-deployconf) and re-importing.\n"
						+ "###############################################################################"
				);
				publishWarningMessage(title, text);
			}

			if (!missingSchemaFile.isEmpty()) {

				final String title = "Missing Schema file(s)";
				final String text = "The following schema methods/functions require file(s) to be present in the tree-based schema export.<br>"
						+ "Because those files are missing, the functionality will not be available after importing!<br>"
						+ "The most common cause is that someone forgot to add these files to the repository."
						+ "<ul><li>" + String.join("</li><li>",  missingSchemaFile) + "</li></ul>";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tThe following schema methods/functions require file(s) to be present in the tree-based schema export.\n"
						+ "\tBecause those files are missing, the functionality will not be available after importing!\n"
						+ "\tThe most common cause is that someone forgot to add these files to the repository.\n\n"
						+ "\t" + String.join("\n\t",  missingSchemaFile)
						+ "\n###############################################################################"
				);
				publishWarningMessage(title, text);

			}

			final long endTime = System.currentTimeMillis();
			DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

			customHeaders.put("end", new Date(endTime).toString());
			customHeaders.put("duration", duration);

			logger.info("Import from {} done. (Took {})", source.toString(), duration);

			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			publishEndMessage(DEPLOYMENT_IMPORT_STATUS, broadcastData);

		} catch (ImportPreconditionFailedException ipfe) {

			logger.warn("Deployment Import not started: {}", ipfe.getMessage());
			publishWarningMessage("Deployment Import not started", ipfe.getMessage());

		} catch (Throwable t) {

			publishWarningMessage("Fatal Error", "Something went wrong - the deployment import has stopped. Please see the log for more information");

			throw t;

		} finally {

			// log collected warnings at the end so they dont get lost
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

		if (target.isAbsolute() != true) {

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

			final Path grantsConf                          = target.resolve(GRANTS_FILE_PATH);
			final Path corsSettingsConf                    = target.resolve(CORS_SETTINGS_FILE_PATH);
			final Path filesConf                           = target.resolve(FILES_FILE_PATH);
			final Path sitesConf                           = target.resolve(SITES_FILE_PATH);
			final Path pagesConf                           = target.resolve(PAGES_FILE_PATH);
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

				writeStringToFile(preDeployConf, "{\n\t// automatically created " + preDeployConf.getFileName() + ". This file is interpreted as a script and run before the application deployment process. To learn more about this, please have a look at the documentation.\n}");
			}

			if (!Files.exists(postDeployConf)) {

				writeStringToFile(postDeployConf, "{\n\t// automatically created " + postDeployConf.getFileName() + ". This file is interpreted as a script and run after the application deployment process. To learn more about this, please have a look at the documentation.\n}");
			}

			writeDeploymentConfigurationFile(deploymentConfFile);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Files");
			exportFiles(files, filesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Sites");
			exportSites(sitesConf);

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

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Resource Access Grants");
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

			logger.info("Export to {} done. (Took {})", target.toString(), duration);

			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			publishEndMessage(DEPLOYMENT_EXPORT_STATUS, broadcastData);


		} catch (FileAlreadyExistsException faee) {

			final String deploymentTargetIsAFileError = "A file already exists at given path - this should be a directory or not exist at all!";

			logger.warn(deploymentTargetIsAFileError + "" + target.toString());

			publishWarningMessage("Fatal Error", deploymentTargetIsAFileError + "<br>" + target.toString());

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

		} catch (Exception ex) {

			logger.warn("can't set group {} for deployment export files: {}", groupName, ex.getMessage());
		}
	}

	private void exportFiles(final Path target, final Path configTarget) throws FrameworkException {

		logger.info("Exporting files (unchanged files will be skipped)");

		final PropertyKey<Boolean> inclKey  = StructrApp.key(File.class, "includeInFrontendExport");
		final PropertyKey<Boolean> jsKey    = StructrApp.key(File.class, "useAsJavascriptLibrary");
		final PropertyKey<Folder> parentKey = StructrApp.key(File.class, "parent");
		final Map<String, Object> config    = new TreeMap<>();
		final App app                       = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// fetch toplevel folders and recurse
			for (final Folder folder : app.nodeQuery(Folder.class).and(parentKey, null).sort(Folder.name).and(inclKey, true).getAsList()) {
				exportFilesAndFolders(target, folder, config);
			}

			// fetch toplevel files that are marked for export or for use as a javascript library
			for (final File file : app.nodeQuery(File.class)
				.and(parentKey, null)
				.sort(File.name)
				.and()
					.or(inclKey, true)
					.or(jsKey, true)
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

	private void exportFilesAndFolders(final Path target, final Folder folder, final Map<String, Object> config) throws IOException {

		// ignore folders with mounted content
		if (folder.isMounted()) {
			return;
		}

		final String name                    = folder.getName();
		final Path path                      = target.resolve(name);

		final Map<String, Object> properties = new TreeMap<>();

		Files.createDirectories(path);

		exportFileConfiguration(folder, properties);

		if (!properties.isEmpty()) {
			String folderPath = folder.getPath();
			config.put(folderPath, properties);
		}

		final List<Folder> folders = Iterables.toList(folder.getFolders());
		Collections.sort(folders, AbstractNode.name.sorted(false));

		for (final Folder child : folders) {
			exportFilesAndFolders(path, child, config);
		}

		final List<File> files = Iterables.toList(folder.getFiles());
		Collections.sort(files, AbstractNode.name.sorted(false));

		for (final File file : files) {
			exportFile(path, file, config);
		}
	}

	protected void exportFile(final Path target, final File file, final Map<String, Object> config) throws IOException {

		final Map<String, Object> properties = new TreeMap<>();
		final String name                    = file.getName();
		final Path src                       = file.getFileOnDisk().toPath();
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
				Files.copy(src, targetPath, StandardCopyOption.REPLACE_EXISTING);

			} catch (IOException ioex) {
				logger.warn("Unable to write file {}: {}", targetPath.toString(), ioex.getMessage());
			}
		}

		exportFileConfiguration(file, properties);

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

			for (final Site site : app.nodeQuery(Site.class).sort(Site.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				sites.add(entry);

				entry.put("id",       site.getProperty(Site.id));
				entry.put("name",     site.getName());
				entry.put("hostname", site.getHostname());
				entry.put("port",     site.getPort());
				entry.put("visibleToAuthenticatedUsers", site.getProperty(Site.visibleToAuthenticatedUsers));
				entry.put("visibleToPublicUsers",        site.getProperty(Site.visibleToPublicUsers));

				final List<String> pageNames = new LinkedList<>();
				for (final Page page : (Iterable<Page>)site.getProperty(StructrApp.key(Site.class, "pages"))) {
					pageNames.add(page.getName());
				}
				entry.put("pages", pageNames);

				exportOwnershipAndSecurity(site, entry);
			}

			tx.success();
		}

		writeJsonToFile(target, sites);
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

			for (final Page page : app.nodeQuery(Page.class).sort(Page.name).getAsList()) {

				if (!(page instanceof ShadowDocument)) {

					final String content = page.getContent(RenderContext.EditMode.DEPLOYMENT);
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

			final ShadowDocument shadowDocument = app.nodeQuery(ShadowDocument.class).getFirst();
			if (shadowDocument != null) {

				for (final DOMNode node : shadowDocument.getElements()) {

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
			for (final Template template : app.nodeQuery(Template.class).getAsList()) {

				final boolean isShared    = template.getProperty(StructrApp.key(DOMNode.class, "sharedComponent")) != null;
				final boolean inTrash     = template.inTrash();

				if (inTrash || isShared) {
					continue;
				}

				final String content = template.getProperty(StructrApp.key(Template.class, "content"));

				exportContentElementSource(targetFolder, template, configuration, content);
			}

			tx.success();
		}

		writeJsonToFile(configTarget, configuration);
	}

	/**
	 * Consolidated export method for Content and Template
	 */
	private void exportContentElementSource(final Path targetFolder, final DOMNode node, final Map<String, Object> configuration, final String content) throws FrameworkException {

		if (content != null) {

			// name with uuid or just uuid
			String name = node.getProperty(AbstractNode.name);
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

	private void exportResourceAccessGrants(final Path target) throws FrameworkException {

		logger.info("Exporting resource access grants");

		final List<Map<String, Object>> grants = new LinkedList<>();
		final App app                          = StructrApp.getInstance();

		final List<String> unreachableGrants = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess res : app.nodeQuery(ResourceAccess.class).sort(ResourceAccess.signature).getAsList()) {

				final Map<String, Object> grant = new TreeMap<>();
				grants.add(grant);

				grant.put("id",        res.getProperty(ResourceAccess.id));
				grant.put("signature", res.getProperty(ResourceAccess.signature));
				grant.put("flags",     res.getProperty(ResourceAccess.flags));
				grant.put("visibleToPublicUsers",        res.isVisibleToPublicUsers());
				grant.put("visibleToAuthenticatedUsers", res.isVisibleToAuthenticatedUsers());

				exportSecurity(res, grant);

				final List grantees = (List)grant.get("grantees");

				if (res.getProperty(ResourceAccess.flags) > 0 && res.isVisibleToPublicUsers() == false && res.isVisibleToAuthenticatedUsers() == false && grantees.isEmpty()) {
					unreachableGrants.add(res.getProperty(ResourceAccess.signature));
				}
			}

			tx.success();
		}

		if (!unreachableGrants.isEmpty()) {

			final String text = "Found configured but unreachable grant(s)! The ability to use group/user rights to grants has been added to improve flexibility.\n\n  The following grants are inaccessible for any non-admin users:\n\n"
					+ unreachableGrants.stream().reduce( "", (acc, signature) -> acc.concat("  - ").concat(signature).concat("\n"))
					+ "\n  You can edit the visibility in the 'Security' area.\n";

			final String htmlText = "The ability to use group/user rights to grants has been added to improve flexibility. The following grants are inaccessible for any non-admin users:<br><br>"
					+ unreachableGrants.stream().reduce( "", (acc, signature) -> acc.concat("&nbsp;- ").concat(signature).concat("<br>"))
					+ "<br>You can edit the visibility in the <a href=\"#security\">Security</a> area.";

			deferredLogTexts.add(text);
			publishWarningMessage("Found configured but unreachable grant(s)", htmlText);
		}

		writeSortedCompactJsonToFile(target, grants, null);
	}

	private void exportCorsSettings(final Path target) throws FrameworkException {

		logger.info("Exporting CORS Settings");

		final List<Map<String, Object>> corsSettings = new LinkedList<>();
		final App app                           = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final CorsSetting corsSetting : app.nodeQuery(CorsSetting.class).sort(CorsSetting.requestUri).getAsList()) {

				final Map<String, Object> entry = new LinkedHashMap<>();
				corsSettings.add(entry);

				putData(entry, "id",               corsSetting.getProperty(CorsSetting.id));
				putData(entry, "requestUri",       corsSetting.getProperty(StructrApp.key(CorsSetting.class, "requestUri")));
				putData(entry, "acceptedOrigins",  corsSetting.getProperty(StructrApp.key(CorsSetting.class, "acceptedOrigins")));
				putData(entry, "maxAge",           corsSetting.getProperty(StructrApp.key(CorsSetting.class, "maxAge")));
				putData(entry, "allowMethods",     corsSetting.getProperty(StructrApp.key(CorsSetting.class, "allowMethods")));
				putData(entry, "allowHeaders",     corsSetting.getProperty(StructrApp.key(CorsSetting.class, "allowHeaders")));
				putData(entry, "allowCredentials", corsSetting.getProperty(StructrApp.key(CorsSetting.class, "allowCredentials")));
				putData(entry, "exposeHeaders",    corsSetting.getProperty(StructrApp.key(CorsSetting.class, "exposeHeaders")));
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

				// move global schema methods to files
				final List<Map<String, Object>> globalSchemaMethods = schema.getGlobalMethods();

				if (!globalSchemaMethods.isEmpty()) {

					final Path globalMethodsFolder = Files.createDirectories(targetFolder.resolve(DEPLOYMENT_SCHEMA_GLOBAL_METHODS_FOLDER));

					for (Map<String, Object> schemaMethod : globalSchemaMethods) {

						final String methodName            = (String) schemaMethod.get("name");

						final String methodSource          = (String) schemaMethod.get(DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY);
						final Path globalMethodSourceFile  = globalMethodsFolder.resolve(methodName);

						final String relativeSourceFilePath  = "./" + targetFolder.relativize(globalMethodSourceFile).toString();

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
									fp.setReadFunction("./" + targetFolder.relativize(readFunctionFile).toString());
								}

								final Path writeFunctionFile = functionsFolder.resolve(fp.getName() + DEPLOYMENT_SCHEMA_WRITE_FUNCTION_SUFFIX);
								final String writeFunction   = fp.getWriteFunction();

								if (writeFunction != null) {
									writeStringToFile(writeFunctionFile, writeFunction);
									fp.setWriteFunction("./" + targetFolder.relativize(writeFunctionFile).toString());
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
									method.setSource("./" + targetFolder.relativize(methodSourceFile).toString());
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

	private void exportConfiguration(final DOMNode node, final Map<String, Object> config) throws FrameworkException {

		putData(config, "id",                          node.getProperty(DOMNode.id));
		putData(config, "visibleToPublicUsers",        node.isVisibleToPublicUsers());
		putData(config, "visibleToAuthenticatedUsers", node.isVisibleToAuthenticatedUsers());

		if (node instanceof Content) {
			putData(config, "contentType", node.getProperty(StructrApp.key(Content.class, "contentType")));
		}

		if (node instanceof Template) {

			// mark this template as being shared
			putData(config, "shared", Boolean.toString(node.isSharedComponent() && node.getParent() == null));
		}

		if (node instanceof Page) {

			putData(config, "basicAuthRealm",          node.getProperty(StructrApp.key(Page.class, "basicAuthRealm")));
			putData(config, "cacheForSeconds",         node.getProperty(StructrApp.key(Page.class, "cacheForSeconds")));
			putData(config, "category",                node.getProperty(StructrApp.key(Page.class, "category")));
			putData(config, "contentType",             node.getProperty(StructrApp.key(Page.class, "contentType")));
			putData(config, "dontCache",               node.getProperty(StructrApp.key(Page.class, "dontCache")));
			putData(config, "enableBasicAuth",         node.getProperty(StructrApp.key(Page.class, "enableBasicAuth")));
			putData(config, "hidden",                  node.getProperty(StructrApp.key(Page.class, "hidden")));
			putData(config, "hideConditions",          node.getProperty(StructrApp.key(Page.class, "hideConditions")));
			putData(config, "pageCreatesRawData",      node.getProperty(StructrApp.key(Page.class, "pageCreatesRawData")));
			putData(config, "path",                    node.getProperty(StructrApp.key(Page.class, "path")));
			putData(config, "position",                node.getProperty(StructrApp.key(Page.class, "position")));
			putData(config, "showConditions",          node.getProperty(StructrApp.key(Page.class, "showConditions")));
			putData(config, "showOnErrorCodes",        node.getProperty(StructrApp.key(Page.class, "showOnErrorCodes")));

		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(node.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (!key.isPartOfBuiltInSchema() && key.relatedType() == null && !(key instanceof FunctionProperty) && !(key instanceof CypherProperty)) {

				putData(config, key.jsonName(), node.getProperty(key));
			}
		}
	}

	protected void exportFileConfiguration(final AbstractFile abstractFile, final Map<String, Object> config) {

		putData(config, "id",                          abstractFile.getProperty(AbstractFile.id));
		putData(config, "visibleToPublicUsers",        abstractFile.isVisibleToPublicUsers());
		putData(config, "visibleToAuthenticatedUsers", abstractFile.isVisibleToAuthenticatedUsers());

		putData(config, "type",                        abstractFile.getProperty(File.type));

		if (abstractFile instanceof File) {

			final File file = (File)abstractFile;

			putData(config, "isTemplate", file.isTemplate());
			putData(config, "dontCache", abstractFile.getProperty(StructrApp.key(File.class, "dontCache")));
		}

		if (abstractFile instanceof Indexable) {
			putData(config, "contentType",                 abstractFile.getProperty(StructrApp.key(File.class, "contentType")));
		}

		if (abstractFile instanceof File) {
			putData(config, "cacheForSeconds",             abstractFile.getProperty(StructrApp.key(File.class, "cacheForSeconds")));
		}

		if (abstractFile instanceof JavaScriptSource) {
			putData(config, "useAsJavascriptLibrary",      abstractFile.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));
		}

		putData(config, "includeInFrontendExport",     abstractFile.getProperty(StructrApp.key(File.class, "includeInFrontendExport")));

		if (abstractFile instanceof Linkable) {
			putData(config, "basicAuthRealm",              abstractFile.getProperty(StructrApp.key(File.class, "basicAuthRealm")));
			putData(config, "enableBasicAuth",             abstractFile.getProperty(StructrApp.key(File.class, "enableBasicAuth")));
		}

		if (abstractFile instanceof Image) {

			final Image image = (Image)abstractFile;

			putData(config, "isThumbnail",             image.isThumbnail());
			putData(config, "isImage",                 image.isImage());
			putData(config, "width",                   image.getWidth());
			putData(config, "height",                  image.getHeight());
		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(abstractFile.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (!key.isPartOfBuiltInSchema() && key.relatedType() == null && !(key instanceof FunctionProperty) && !(key instanceof CypherProperty)) {

				putData(config, key.jsonName(), abstractFile.getProperty(key));
			}
		}

		exportOwnershipAndSecurity(abstractFile, config);
	}

	protected void exportOwnershipAndSecurity(final NodeInterface node, final Map<String, Object> config) {

		// export owner
		final Map<String, Object> map = new HashMap<>();
		final Principal owner         = node.getOwnerNode();

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
		for (final Security security : node.getSecurityRelationships()) {

			if (security != null) {

				final Map<String, Object> grant = new TreeMap<>();

				grant.put("name", security.getSourceNode().getProperty(AbstractNode.name));
				final String allowedActions = StringUtils.join(security.getPermissions(), ",");
				grant.put("allowed", allowedActions);

				if (allowedActions.length() > 0) {
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
				final String ownerName = (String) ((Map)entry.get("owner")).get("name");
				final Principal owner = StructrApp.getInstance().nodeQuery(Principal.class).andName(ownerName).getFirst();

				if (owner == null) {
					logger.warn("Unknown owner {}, ignoring.", ownerName);
					DeployCommand.addMissingPrincipal(ownerName);

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

				final String granteeName = (String) grantee.get("name");
				final Principal owner    = StructrApp.getInstance().nodeQuery(Principal.class).andName(granteeName).getFirst();

				if (owner == null) {

					logger.warn("Unknown grantee {}, ignoring.", granteeName);
					DeployCommand.addMissingPrincipal(granteeName);

				} else {

					cleanedGrantees.add(grantee);
				}
			}

			entry.put("grantees", cleanedGrantees);
		}
	}

	private void exportMailTemplates(final Path targetConf, final Path targetFolder) throws FrameworkException {

		logger.info("Exporting mail templates");

		final PropertyKey<String> textKey             = StructrApp.key(MailTemplate.class, "text");
		final PropertyKey<String> localeKey           = StructrApp.key(MailTemplate.class, "locale");
		final List<Map<String, Object>> mailTemplates = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try {

			deleteDirectoryContentsRecursively(targetFolder);

			try (final Tx tx = app.tx()) {

				for (final MailTemplate mailTemplate : app.nodeQuery(MailTemplate.class).sort(MailTemplate.name).getAsList()) {

					// generate filename for output file
					String filename = mailTemplate.getProperty(MailTemplate.name) + "_-_" + mailTemplate.getProperty(localeKey) + ".html";

					if (Files.exists(targetFolder.resolve(filename))) {
						filename = mailTemplate.getProperty(MailTemplate.name) + "_-_" + mailTemplate.getProperty(localeKey) + "_-_" + mailTemplate.getProperty(MailTemplate.id) + ".html";
					}

					final Map<String, Object> entry = new TreeMap<>();
					mailTemplates.add(entry);

					putData(entry, "id",                          mailTemplate.getProperty(MailTemplate.id));
					putData(entry, "name",                        mailTemplate.getProperty(MailTemplate.name));
					putData(entry, "filename",                    filename);
					putData(entry, "locale",                      mailTemplate.getProperty(localeKey));
					putData(entry, "visibleToAuthenticatedUsers", mailTemplate.getProperty(MailTemplate.visibleToAuthenticatedUsers));
					putData(entry, "visibleToPublicUsers",        mailTemplate.getProperty(MailTemplate.visibleToPublicUsers));

					final Path mailTemplateFile = targetFolder.resolve(filename);
					writeStringToFile(mailTemplateFile, mailTemplate.getProperty(textKey));
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

			for (final Widget widget : app.nodeQuery(Widget.class).sort(Widget.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				widgets.add(entry);

				putData(entry, "id",                          widget.getProperty(Widget.id));
				putData(entry, "name",                        widget.getProperty(Widget.name));
				putData(entry, "visibleToAuthenticatedUsers", widget.getProperty(Widget.visibleToAuthenticatedUsers));
				putData(entry, "visibleToPublicUsers",        widget.getProperty(Widget.visibleToPublicUsers));
				putData(entry, "source",                      widget.getProperty(StructrApp.key(Widget.class, "source")));
				putData(entry, "description",                 widget.getProperty(StructrApp.key(Widget.class, "description")));
				putData(entry, "isWidget",                    widget.getProperty(StructrApp.key(Widget.class, "isWidget")));
				putData(entry, "treePath",                    widget.getProperty(StructrApp.key(Widget.class, "treePath")));
				putData(entry, "configuration",               widget.getProperty(StructrApp.key(Widget.class, "configuration")));
				putData(entry, "isPageTemplate",              widget.getProperty(StructrApp.key(Widget.class, "isPageTemplate")));
				putData(entry, "selectors",                   widget.getProperty(StructrApp.key(Widget.class, "selectors")));
			}

			tx.success();
		}

		writeJsonToFile(target, widgets);
	}

	private void exportApplicationConfigurationData(final Path target) throws FrameworkException {

		logger.info("Exporting application configuration data");

		final List<Map<String, Object>> applicationConfigurationDataNodes = new LinkedList<>();
		final App app                                                     = StructrApp.getInstance();

		final PropertyKey<String> configTypeKey = StructrApp.key(ApplicationConfigurationDataNode.class, "configType");
		final PropertyKey<String> contentKey    = StructrApp.key(ApplicationConfigurationDataNode.class, "content");

		try (final Tx tx = app.tx()) {

			for (final ApplicationConfigurationDataNode acdn : app.nodeQuery(ApplicationConfigurationDataNode.class).sort(configTypeKey).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				applicationConfigurationDataNodes.add(entry);

				entry.put("id",         acdn.getProperty(ApplicationConfigurationDataNode.id));
				entry.put("name",       acdn.getProperty(ApplicationConfigurationDataNode.name));
				entry.put("configType", acdn.getProperty(configTypeKey));
				entry.put("content",    acdn.getProperty(contentKey));

				exportOwnershipAndSecurity(acdn, entry);
			}

			tx.success();
		}

		writeSortedCompactJsonToFile(target, applicationConfigurationDataNodes, new AbstractMapComparator<Object>() {
			@Override
			public String getKey (Map<String, Object> map) {

				final Object configType = map.get("configType");
				final Object name       = map.get("name");
				final Object id         = map.get("id");

				return (configType != null ? configType.toString() : "00-configType").concat((name != null ? name.toString() : "00-name")).concat(id.toString());
			}
		});
	}

	private void exportLocalizations(final Path target) throws FrameworkException {

		logger.info("Exporting localizations");

		final PropertyKey<String> localizedNameKey    = StructrApp.key(Localization.class, "localizedName");
		final PropertyKey<String> domainKey           = StructrApp.key(Localization.class, "domain");
		final PropertyKey<String> localeKey           = StructrApp.key(Localization.class, "locale");
		final PropertyKey<String> importedKey         = StructrApp.key(Localization.class, "imported");
		final List<Map<String, Object>> localizations = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final Localization localization : app.nodeQuery(Localization.class).sort(Localization.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>(new IdFirstComparator());

				localizations.add(entry);

				entry.put("id",                          localization.getProperty(Localization.id));
				entry.put("name",                        localization.getProperty(Localization.name));
				entry.put("localizedName",               localization.getProperty(localizedNameKey));
				entry.put("domain",                      localization.getProperty(domainKey));
				entry.put("locale",                      localization.getProperty(localeKey));
				entry.put("imported",                    localization.getProperty(importedKey));
				entry.put("visibleToAuthenticatedUsers", localization.getProperty(MailTemplate.visibleToAuthenticatedUsers));
				entry.put("visibleToPublicUsers",        localization.getProperty(MailTemplate.visibleToPublicUsers));
			}

			tx.success();
		}

		writeSortedCompactJsonToFile(target, localizations, new AbstractMapComparator<Object>() {
			@Override
			public String getKey (Map<String, Object> map) {

				final Object name   = map.get("name");
				final Object domain = map.get("domain");
				final Object locale = map.get("locale");
				final Object id     = map.get("id");

				// null domain is replaced by a string so that those localizations are shown first
				return (name != null ? name.toString() : "null").concat((domain != null ? domain.toString() : "00-nulldomain")).concat((locale != null ? locale.toString() : "null")).concat(id.toString());
			}
		});
	}

	private void exportActionMapping(final Path target) throws FrameworkException {

		logger.info("Exporting action mapping");

		final List<Map<String, Object>> actionMappings    = new LinkedList<>();

		final App app                                     = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ActionMapping actionMapping : app.nodeQuery(ActionMapping.class).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				actionMappings.add(entry);

				putData(entry, "id",                          actionMapping.getProperty(ActionMapping.id));
				putData(entry, "name",                        actionMapping.getProperty(ActionMapping.name));
				putData(entry, "visibleToAuthenticatedUsers", actionMapping.getProperty(ActionMapping.visibleToAuthenticatedUsers));
				putData(entry, "visibleToPublicUsers",        actionMapping.getProperty(ActionMapping.visibleToPublicUsers));

				final PropertyKey<Iterable<DOMElement>> triggerElementsKey = StructrApp.key(ActionMapping.class, "triggerElements");
				List<DOMElement> triggerElements = Iterables.toList(actionMapping.getProperty(triggerElementsKey));

				if (!triggerElements.isEmpty()) {
					putData(entry, "triggerElements", triggerElements.stream().map(domElement -> domElement.getUuid()).collect(Collectors.toList()));
				}

				final PropertyKey<Iterable<DOMNode>> successTargetsKey = StructrApp.key(ActionMapping.class, "successTargets");
				List<DOMNode> successTargets = Iterables.toList(actionMapping.getProperty(successTargetsKey));

				if (!successTargets.isEmpty()) {
					putData(entry, "successTargets", successTargets.stream().map(domNode -> domNode.getUuid()).collect(Collectors.toList()));
				}

				final PropertyKey<Iterable<DOMNode>> failureTargetsKey = StructrApp.key(ActionMapping.class, "failureTargets");
				List<DOMNode> failureTargets = Iterables.toList(actionMapping.getProperty(failureTargetsKey));

				if (!failureTargets.isEmpty()) {
					putData(entry, "failureTargets", failureTargets.stream().map(domNode -> domNode.getUuid()).collect(Collectors.toList()));
				}

				final PropertyKey<Iterable<ParameterMapping>> parameterMappingsKey = StructrApp.key(ActionMapping.class, "parameterMappings");
				List<ParameterMapping> parameterMappings = Iterables.toList(actionMapping.getProperty(parameterMappingsKey));

				if (!parameterMappings.isEmpty()) {
					putData(entry, "parameterMappings", parameterMappings.stream().map(parameterMapping -> parameterMapping.getUuid() ).collect(Collectors.toList()));
				}

				putData(entry, "event",        actionMapping.getProperty(StructrApp.key(ActionMapping.class, "event")));
				putData(entry, "action",       actionMapping.getProperty(StructrApp.key(ActionMapping.class, "action")));
				putData(entry, "method",       actionMapping.getProperty(StructrApp.key(ActionMapping.class, "method")));
				putData(entry, "dataType",     actionMapping.getProperty(StructrApp.key(ActionMapping.class, "dataType")));
				putData(entry, "idExpression", actionMapping.getProperty(StructrApp.key(ActionMapping.class, "idExpression")));

				putData(entry, "successBehaviour", actionMapping.getProperty(StructrApp.key(ActionMapping.class, "successBehaviour")));
				putData(entry, "successPartial",   actionMapping.getProperty(StructrApp.key(ActionMapping.class, "successPartial")));
				putData(entry, "successURL",       actionMapping.getProperty(StructrApp.key(ActionMapping.class, "successURL")));
				putData(entry, "successEvent",     actionMapping.getProperty(StructrApp.key(ActionMapping.class, "successEvent")));

				putData(entry, "failureBehaviour", actionMapping.getProperty(StructrApp.key(ActionMapping.class, "failureBehaviour")));
				putData(entry, "failurePartial",   actionMapping.getProperty(StructrApp.key(ActionMapping.class, "failurePartial")));
				putData(entry, "failureURL",       actionMapping.getProperty(StructrApp.key(ActionMapping.class, "failureURL")));
				putData(entry, "successEvent",     actionMapping.getProperty(StructrApp.key(ActionMapping.class, "failureEvent")));

			}
		}

		writeJsonToFile(target, actionMappings);

	}

	private void exportParameterMapping(final Path target) throws FrameworkException {

		logger.info("Exporting parameter mapping");

		final List<Map<String, Object>> parameterMappings = new LinkedList<>();
		final App app                                     = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ParameterMapping parameterMapping : app.nodeQuery(ParameterMapping.class).sort(ParameterMapping.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				parameterMappings.add(entry);


				putData(entry, "id",                          parameterMapping.getProperty(ParameterMapping.id));
				putData(entry, "name",                        parameterMapping.getProperty(ParameterMapping.name));
				putData(entry, "visibleToAuthenticatedUsers", parameterMapping.getProperty(ParameterMapping.visibleToAuthenticatedUsers));
				putData(entry, "visibleToPublicUsers",        parameterMapping.getProperty(ParameterMapping.visibleToPublicUsers));

				putData(entry, "parameterType",    parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "parameterType")));
				putData(entry, "parameterName",    parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "parameterName")));
				putData(entry, "constantValue",    parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "constantValue")));
				putData(entry, "scriptExpression", parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "scriptExpression")));

				DOMElement inputElement = ((DOMElement) parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "inputElement")));
				if (inputElement != null) {
					putData(entry, "inputElement", inputElement.getUuid());
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

	protected List<Map<String, Object>> readConfigList(final Path conf) {

		try (final Reader reader = Files.newBufferedReader(conf, Charset.forName("utf-8"))) {

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

				String confSource = new String(Files.readAllBytes(confFile), Charset.forName("utf-8")).trim();

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

	private <T extends NodeInterface> void importListData(final Class<T> type, final List<Map<String, Object>> data, final PropertyMap... additionalData) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final T toDelete : app.nodeQuery(type).getAsList()) {
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

			logger.error("Unable to import {}, aborting with {}", type.getSimpleName(), fex.getMessage(), fex);

			throw fex;
		}
	}

	private void importResourceAccessGrants(final Path grantsMetadataFile) throws FrameworkException {

		if (Files.exists(grantsMetadataFile)) {

			logger.info("Reading {}", grantsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing resource access grants");

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

			for (final ResourceAccess toDelete : app.nodeQuery(ResourceAccess.class).getAsList()) {
				app.delete(toDelete);
			}

			for (final Map<String, Object> entry : data) {

				if (!entry.containsKey("grantees") && !entry.containsKey("visibleToPublicUsers") && !entry.containsKey("visibleToAuthenticatedUsers")) {

					isOldExport = true;

					final long flags = ((Number)entry.get("flags")).longValue();
					if (flags != 0) {

						final String signature = (String)entry.get("signature");

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
							grantMessagesHtml.append("Signature <b>").append(signature).append("</b> is probably misconfigured and <b><u>should be split into two grants</u></b>.<br>");
							grantMessagesText.append("    Signature '").append(signature).append("' is probably misconfigured and **should be split into two grants**.\n");
						}

						entry.put("visibleToAuthenticatedUsers", hasAnyAuthFlags);
						entry.put("visibleToPublicUsers",        hasAnyNonAuthFlags);
					}

				} else {
					checkOwnerAndSecurity(entry);
				}

				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, ResourceAccess.class, entry);

				app.create(ResourceAccess.class, map);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import resouce access grant, aborting with {}", fex.getMessage(), fex);

			throw fex;

		} finally {

			if (isOldExport) {

				final String text = "Found outdated version of grants.json file without visibility and grantees!\n\n"
						+ "    Configuration was auto-updated using this simple heuristic:\n"
						+ "     * Grants with public access were set to **visibleToPublicUsers: true**\n"
						+ "     * Grants with authenticated access were set to **visibleToAuthenticatedUsers: true**\n\n"
						+ "    Please make any necessary changes in the 'Security' area as this may not suffice for your use case. The ability to use group/user rights to grants has been added to improve flexibility.";

				final String htmlText = "Configuration was auto-updated using this simple heuristic:<br>"
						+ "&nbsp;- Grants with public access were set to <code>visibleToPublicUsers: true</code><br>"
						+ "&nbsp;- Grants with authenticated access were set to <code>visibleToAuthenticatedUsers: true</code><br><br>"
						+ "Please make any necessary changes in the <a href=\"#security\">Security</a> area as this may not suffice for your use case. The ability to use group/user rights to grants has been added to improve flexibility.";

				deferredLogTexts.add(text + "\n\n" + grantMessagesText);
				publishWarningMessage("Found grants.json file without visibility and grantees", htmlText + "<br><br>" + grantMessagesHtml);
			}
		}
	}

	private void importCorsSettings(final Path corsSettingsMetadataFile) throws FrameworkException {

		if (Files.exists(corsSettingsMetadataFile)) {

			logger.info("Reading {}", corsSettingsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing CORS Settings");

			importListData(CorsSetting.class, readConfigList(corsSettingsMetadataFile));
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

			importListData(MailTemplate.class, mailTemplatesConf);
		}
	}

	private void importWidgets(final Path widgetsMetadataFile) throws FrameworkException {

		if (Files.exists(widgetsMetadataFile)) {

			logger.info("Reading {}", widgetsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing widgets");

			importListData(Widget.class, readConfigList(widgetsMetadataFile));
		}
	}

	private void importLocalizations(final Path localizationsMetadataFile) throws FrameworkException {

		if (Files.exists(localizationsMetadataFile)) {

			final PropertyMap additionalData = new PropertyMap();

			// Question: shouldn't this be true? No, 'imported' is a flag for legacy-localization which
			// have been imported from a legacy-system which was replaced by structr.
			// it is a way to differentiate between new and old localization strings
			additionalData.put(StructrApp.key(Localization.class, "imported"), false);

			logger.info("Reading {}", localizationsMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing localizations");

			importListData(Localization.class, readConfigList(localizationsMetadataFile), additionalData);
		}
	}

	private void importApplicationConfigurationNodes(final Path applicationConfigurationDataMetadataFile) throws FrameworkException {

		if (Files.exists(applicationConfigurationDataMetadataFile)) {

			logger.info("Reading {}", applicationConfigurationDataMetadataFile);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing application configuration data");

			importListData(ApplicationConfigurationDataNode.class, readConfigList(applicationConfigurationDataMetadataFile));
		}
	}

	private void importFiles(final Path filesMetadataFile, final Path source, final SecurityContext ctx) throws FrameworkException {

		if (Files.exists(filesMetadataFile)) {

			final Map<String, Object> filesMetadata      = new HashMap<>();

			logger.info("Reading {}", filesMetadataFile);
			filesMetadata.putAll(readMetadataFileIntoMap(filesMetadataFile));

			final Path files = source.resolve("files");
			if (Files.exists(files)) {

				try {

					logger.info("Importing files (unchanged files will be skipped)");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing files");

					FileImportVisitor fiv = new FileImportVisitor(ctx, files, filesMetadata);
					Files.walkFileTree(files, fiv);

				} catch (IOException ioex) {
					logger.warn("Exception while importing files", ioex);
				}
			}
		}
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

	private void importHTMLContent(final App app, final Path source, final Path pagesMetadataFile, final Path componentsMetadataFile, final Path templatesMetadataFile, final Path sitesConfFile, final boolean extendExistingApp, final boolean relativeVisibility, final Map<DOMNode, PropertyMap> deferredNodesAndTheirProperties) throws FrameworkException {

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
			final Path templates = source.resolve("templates");
			final Path components = source.resolve("components");
			final Path pages = source.resolve("pages");

			// remove all DOMNodes from the database (clean webapp for import, but only
			// if the actual import directories exist, don't delete web components if
			// an empty directory was specified accidentially).
			if (!extendExistingApp && Files.exists(templates) && Files.exists(components) && Files.exists(pages)) {

				try (final Tx tx = app.tx()) {

					tx.disableChangelog();

					logger.info("Removing pages, templates and components");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing pages, templates and components");

					app.deleteAllNodesOfType(DOMNode.class);

					if (Files.exists(sitesConfFile)) {

						logger.info("Removing sites");
						publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing sites");

						app.deleteAllNodesOfType(Site.class);
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
	}

	private void importActionMapping(final Path path) throws FrameworkException {

		if (Files.exists(path)) {

			logger.info("Reading {}", path);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing action mapping");

			importListData(StructrApp.getConfiguration().getNodeEntityClass("ActionMapping"), readConfigList(path));
		}
	}


	private void importParameterMapping(final Path parameterMappingPath) throws FrameworkException {

		if (Files.exists(parameterMappingPath)) {

			logger.info("Reading {}", parameterMappingPath);
			publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing parameter mapping");

			importListData(StructrApp.getConfiguration().getNodeEntityClass("ParameterMapping"), readConfigList(parameterMappingPath));
		}
	}


	private void handleDeferredProperties() throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (final DOMNode node : deferredNodesAndTheirProperties.keySet()) {

				final PropertyMap properties = deferredNodesAndTheirProperties.get(node);

				for (final PropertyKey propertyKey : properties.keySet()) {

					final PropertyConverter inputConverter = propertyKey.inputConverter(securityContext);

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
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			for (Map<String, Object> entry : data) {

				final List<Page> pages = new LinkedList();

				for (final String pageName : (List<String>)entry.get("pages")) {
					pages.add(app.nodeQuery(Page.class).andName(pageName).getFirst());
				}

				entry.remove("pages");

				checkOwnerAndSecurity(entry);

				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, Site.class, entry);

				map.put(StructrApp.key(Site.class, "pages"), pages);

				app.create(Site.class, map);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import site, aborting with {}", fex.getMessage(), fex);

			throw fex;
		}
	}

	private void linkDeferredPages(final App app) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			deferredPageLinks.forEach((String linkableUUID, String pagePath) -> {

				try {
					final DOMNode linkElement = StructrApp.getInstance().get(DOMNode.class, linkableUUID);
					final Linkable linkedPage = StructrApp.getInstance().nodeQuery(Linkable.class).and(StructrApp.key(Page.class, "path"), pagePath).or(Page.name, pagePath).getFirst();

					((LinkSource)linkElement).setLinkable(linkedPage);

				} catch (Throwable t) {
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

						// detect tree-based export (absence of folder "File")
						final boolean isTreeBasedExport = Files.exists(schemaFolder.resolve("File"));

						final StructrSchemaDefinition schema = (StructrSchemaDefinition)StructrSchema.createFromSource(reader);

						if (isTreeBasedExport) {

							final Path globalMethodsFolder = schemaFolder.resolve(DEPLOYMENT_SCHEMA_GLOBAL_METHODS_FOLDER);

							if (Files.exists(globalMethodsFolder)) {

								for (Map<String, Object> schemaMethod : schema.getGlobalMethods()) {

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

											if (propDef instanceof StructrFunctionProperty) {

												final StructrFunctionProperty fp = (StructrFunctionProperty)propDef;

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

												// new export name includes the number of parameters
												final Path methodSourceFile = methodsFolder.resolve(uniqueMethodName);
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
				throw new FrameworkException(422, fex.getMessage(), fex.getErrorBuffer());

			} catch (Throwable t) {
				logger.warn("Unable to import schema: {}", t.getMessage());
			}
		}
	}

	public boolean isDOMNodeVisibilityRelativeToParent(final Map<String, String> deploymentConf) {
		return DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_PARENT_VALUE.equals(deploymentConf.get(DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_KEY));
	}

	protected Map<String, String> readDeploymentConfigurationFile (final Path confFile) {

		final Map<String, String> deploymentConf = new LinkedHashMap<>();

		if (Files.exists(confFile)) {

			try {

				final PropertiesConfiguration config = new PropertiesConfiguration(confFile.toFile());
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

			final PropertiesConfiguration config = new PropertiesConfiguration();

			config.setProperty(DEPLOYMENT_VERSION_KEY,                         VersionHelper.getFullVersionInfo());
			config.setProperty(DEPLOYMENT_DATE_KEY,                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
			config.setProperty(DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_KEY, DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_PARENT_VALUE);

			config.save(confFile.toFile());

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

	void deleteRecursively(final Path path) throws IOException {

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

	private boolean acceptDeploymentExportVersion(final Map<String, String> deploymentConfig) {

		final int currentVersion = parseVersionString(VersionHelper.getFullVersionInfo());
		final int exportVersion  = parseVersionString(deploymentConfig.get(DEPLOYMENT_VERSION_KEY));

		return currentVersion >= exportVersion;
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

	// ----- public static methods -----
	public static void addDeferredPagelink (String linkableUUID, String pagePath) {
		deferredPageLinks.put(linkableUUID, pagePath);
	}

	public static void updateDeferredPagelink (String initialUUID, String correctUUID) {

		if (deferredPageLinks.containsKey(initialUUID)) {
			deferredPageLinks.put(correctUUID, deferredPageLinks.get(initialUUID));
			deferredPageLinks.remove(initialUUID);
		}
	}

	public static void addMissingPrincipal (final String principalName) {
		missingPrincipals.add(principalName);
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
				Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(this.group);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
			try {
				Files.getFileAttributeView(dir, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(this.group);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return CONTINUE;
		}
	}
}
