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
import com.google.gson.GsonBuilder;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
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
import org.structr.core.StaticValue;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Localization;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.Security;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.module.StructrModule;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.rest.serialization.StreamingJsonWriter;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrFunctionProperty;
import org.structr.schema.export.StructrMethodDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.export.StructrSchemaDefinition;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.web.common.AbstractMapComparator;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.AbstractMinifiedFile;
import org.structr.web.entity.ApplicationConfigurationDataNode;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.MinifiedCssFile;
import org.structr.web.entity.MinifiedJavaScriptFile;
import org.structr.web.entity.Site;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.maintenance.deploy.ComponentImportVisitor;
import org.structr.web.maintenance.deploy.FileImportVisitor;
import org.structr.web.maintenance.deploy.ImportFailureException;
import org.structr.web.maintenance.deploy.PageImportVisitor;
import org.structr.web.maintenance.deploy.TemplateImportVisitor;
import org.structr.websocket.command.CreateComponentCommand;

public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger                     = LoggerFactory.getLogger(DeployCommand.class.getName());
	private static final Pattern pattern                   = Pattern.compile("[a-f0-9]{32}");

	private static final Map<String, String> deferredPageLinks = new LinkedHashMap<>();
	protected static final Set<String> missingPrincipals       = new HashSet<>();
	protected static final Set<String> missingSchemaFile       = new HashSet<>();

	protected static final AtomicBoolean deploymentActive      = new AtomicBoolean(false);

	private final static String DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_KEY          = "visibility-flags-relative-to";
	private final static String DEPLOYMENT_DOM_NODE_VISIBILITY_RELATIVE_TO_PARENT_VALUE = "parent";
	private final static String DEPLOYMENT_VERSION_KEY                                  = "structr-version";
	private final static String DEPLOYMENT_DATE_KEY                                     = "deployment-date";

	private final static String DEPLOYMENT_IMPORT_STATUS   = "DEPLOYMENT_IMPORT_STATUS";
	private final static String DEPLOYMENT_EXPORT_STATUS   = "DEPLOYMENT_EXPORT_STATUS";

	private final static String DEPLOYMENT_SCHEMA_GLOBAL_METHODS_FOLDER = "_globalMethods";
	private final static String DEPLOYMENT_SCHEMA_METHODS_FOLDER        = "methods";
	private final static String DEPLOYMENT_SCHEMA_FUNCTIONS_FOLDER      = "functions";
	private final static String DEPLOYMENT_SCHEMA_READ_FUNCTION_SUFFIX  = ".readFunction";
	private final static String DEPLOYMENT_SCHEMA_WRITE_FUNCTION_SUFFIX = ".writeFunction";
	private final static String DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY  = "source";

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
		return new StreamingJsonWriter(new StaticValue<String>(PropertyView.All), true, 1, false);
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

	protected void doImport(final Map<String, Object> attributes) throws FrameworkException {

		// backup previous value of change log setting and disable during deployment
		final boolean changeLogEnabled = Settings.ChangelogEnabled.getValue();
		Settings.ChangelogEnabled.setValue(false);

		try {

			missingPrincipals.clear();
			missingSchemaFile.clear();

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

			final Map<String, Object> componentsMetadata = new HashMap<>();
			final Map<String, Object> templatesMetadata  = new HashMap<>();
			final Map<String, Object> pagesMetadata      = new HashMap<>();
			final Map<String, Object> filesMetadata      = new HashMap<>();

			if (StringUtils.isBlank(path)) {

				throw new FrameworkException(422, "Please provide 'source' attribute for deployment source directory path.");
			}

			final Path source = Paths.get(path);
			if (!Files.exists(source)) {

				publishWarningMessage("Import not started", "Source path " + path + " does not exist.");

				throw new FrameworkException(422, "Source path " + path + " does not exist.");
			}

			if (!Files.isDirectory(source)) {

				publishWarningMessage("Import not started", "Source path '" + path + "' is not a directory.");

				throw new FrameworkException(422, "Source path " + path + " is not a directory.");
			}

			if (source.isAbsolute() != true) {

				publishWarningMessage("Import not started", "Source path '" + path + "' is not an absolute path - relative paths are not allowed.");

				throw new FrameworkException(422, "Source path '" + path + "' is not an absolute path - relative paths are not allowed.");
			}

			logger.info("Importing from '{}'", path);

			// read deployment.conf (file containing information about deployment export)
			final Path deploymentConfFile            = source.resolve("deployment.conf");
			final Map<String, String> deploymentConf = readDeploymentConfigurationFile(deploymentConfFile);
			final boolean relativeVisibility         = isDOMNodeVisibilityRelativeToParent(deploymentConf);

			// version check (don't import deployment exports from newer versions!)
			if (!acceptDeploymentExportVersion(deploymentConf)) {

				final String currentVersion = VersionHelper.getFullVersionInfo();
				final String exportVersion  = StringUtils.defaultIfEmpty(deploymentConf.get(DEPLOYMENT_VERSION_KEY), "pre 3.5");

				final String title = "Incompatible Deployment Import";
				final String text = "The deployment export data currently being imported has been created with a newer version of Structr\n"
						+ "which is not supported because of incompatible changes in the deployment format.\n"
						+ "Current version: " + currentVersion + "\n"
						+ "Export version:  " + exportVersion;
				final String htmlText = "The deployment export data currently being imported has been created with a newer version of Structr<br>"
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

			// visibility check
			if (!relativeVisibility) {

				final String title = "Important Information";
				final String text = "The deployment export data currently being imported has been created with an older version of Structr\n"
						+ "in which the visibility flags of DOM elements were exported depending on the flags of the containing page.\n"
						+ "***The data will be imported correctly, based on the old format.***\n"
						+ "After this import has finished, you should **export again to the same location** so that the deployment export data will be upgraded to the most recent format.";
				final String htmlText = "The deployment export currently being imported has been created with an older version of Structr<br>"
						+ "in which the visibility flags of DOM elements were exported depending on the flags of the containing page.<br>"
						+ "<b>The data will be imported correctly, based on the old format.</b><br>"
						+ "After this import has finished, you should <b>export again to the same location</b> so that the deployment export data will be upgraded to the most recent format.";

				logger.info(title + ": " + text);
				publishWarningMessage(title, htmlText);
			}

			// apply pre-deploy.conf
			applyConfigurationFile(ctx, source.resolve("pre-deploy.conf"), DEPLOYMENT_IMPORT_STATUS);

			// read grants.json
			final Path grantsMetadataFile = source.resolve("security/grants.json");
			if (Files.exists(grantsMetadataFile)) {

				logger.info("Reading {}", grantsMetadataFile);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing resource access grants");

				importListData(ResourceAccess.class, readConfigList(grantsMetadataFile));
			}

			// read schema-methods.json
			final Path schemaMethodsMetadataFile = source.resolve("schema-methods.json");
			if (Files.exists(schemaMethodsMetadataFile)) {

				logger.info("Reading {}", schemaMethodsMetadataFile);
				final String title = "Deprecation warning";
				final String text = "Found file 'schema-methods.json'. Newer versions store global schema methods in the schema snapshot file. Recreate the export with the current version to avoid compatibility issues. Support for importing this file will be dropped in future versions.";

				logger.info(title + ": " + text);
				publishWarningMessage(title, text);

				importListData(SchemaMethod.class, readConfigList(schemaMethodsMetadataFile));
			}

			// read mail-templates.json
			final Path mailTemplatesMetadataFile = source.resolve("mail-templates.json");
			if (Files.exists(mailTemplatesMetadataFile)) {

				logger.info("Reading {}", mailTemplatesMetadataFile);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing mail templates");

				List<Map<String, Object>> mailTemplatesConf = readConfigList(mailTemplatesMetadataFile);

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

			// read widgets.json
			final Path widgetsMetadataFile = source.resolve("widgets.json");
			if (Files.exists(widgetsMetadataFile)) {

				logger.info("Reading {}", widgetsMetadataFile);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing widgets");

				importListData(Widget.class, readConfigList(widgetsMetadataFile));
			}

			// read localizations.json
			final Path localizationsMetadataFile = source.resolve("localizations.json");
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

			// read widgets.json
			final Path applicationConfigurationDataMetadataFile = source.resolve("application-configuration-data.json");
			if (Files.exists(applicationConfigurationDataMetadataFile)) {

				logger.info("Reading {}", applicationConfigurationDataMetadataFile);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing application configuration data");

				importListData(ApplicationConfigurationDataNode.class, readConfigList(applicationConfigurationDataMetadataFile));
			}

			// read files.json
			final Path filesMetadataFile = source.resolve("files.json");
			if (Files.exists(filesMetadataFile)) {

				logger.info("Reading {}", filesMetadataFile);
				filesMetadata.putAll(readMetadataFileIntoMap(filesMetadataFile));
			}

			// read pages.json
			final Path pagesMetadataFile = source.resolve("pages.json");
			if (Files.exists(pagesMetadataFile)) {

				logger.info("Reading {}", pagesMetadataFile);
				pagesMetadata.putAll(readMetadataFileIntoMap(pagesMetadataFile));
			}

			// read components.json
			final Path componentsMetadataFile = source.resolve("components.json");
			if (Files.exists(componentsMetadataFile)) {

				logger.info("Reading {}", componentsMetadataFile);
				componentsMetadata.putAll(readMetadataFileIntoMap(componentsMetadataFile));
			}

			// read templates.json
			final Path templatesMetadataFile = source.resolve("templates.json");
			if (Files.exists(templatesMetadataFile)) {

				logger.info("Reading {}", templatesMetadataFile);
				templatesMetadata.putAll(readMetadataFileIntoMap(templatesMetadataFile));
			}

			// import schema
			final Path schemaFolder = source.resolve("schema");
			if (Files.exists(schemaFolder)) {

				try {

					logger.info("Importing data from schema/ directory");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing schema");

					importSchema(schemaFolder, extendExistingApp);

				} catch (ImportFailureException fex) {

					logger.warn("Unable to import schema: {}", fex.getMessage());
					throw new FrameworkException(422, fex.getMessage(), fex.getErrorBuffer());

				} catch (Throwable t) {
					logger.warn("Unable to import schema: {}", t.getMessage());
				}
			}

			// import files
			final Path files = source.resolve("files");
			if (Files.exists(files)) {

				try {

					logger.info("Importing files (unchanged files will be skipped)");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing files");

					FileImportVisitor fiv = new FileImportVisitor(ctx, files, filesMetadata);
					Files.walkFileTree(files, fiv);
					fiv.handleDeferredFiles();

				} catch (IOException ioex) {
					logger.warn("Exception while importing files", ioex);
				}
			}


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


			// construct paths
			final Path templates  = source.resolve("templates");
			final Path components = source.resolve("components");
			final Path pages      = source.resolve("pages");
			final Path sitesConfFile = source.resolve("sites.json");

			// remove all DOMNodes from the database (clean webapp for import, but only
			// if the actual import directories exist, don't delete web components if
			// an empty directory was specified accidentially).
			if (!extendExistingApp && Files.exists(templates) && Files.exists(components) && Files.exists(pages)) {

				try (final Tx tx = app.tx()) {

					tx.disableChangelog();

					logger.info("Removing pages, templates and components");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing pages, templates and components");

					app.delete(DOMNode.class);

					if (Files.exists(sitesConfFile)) {

						logger.info("Removing sites");
						publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing sites");

						app.delete(Site.class);
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

					Files.walkFileTree(templates, new TemplateImportVisitor(templatesMetadata));

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

					final ComponentImportVisitor visitor = new ComponentImportVisitor(componentsMetadata, relativeVisibility);

					Files.walkFileTree(components, visitor);

					final List<Path> deferredPaths = visitor.getDeferredPaths();
					if (!deferredPaths.isEmpty()) {

						logger.info("Attempting to import deferred components..");

						for (final Path deferred : deferredPaths) {

							visitor.visitFile(deferred, Files.readAttributes(deferred, BasicFileAttributes.class));
						}

						FlushCachesCommand.flushAll();
					}


				} catch (IOException ioex) {
					logger.warn("Exception while importing shared components", ioex);
				}
			}

			// import pages
			if (Files.exists(pages)) {

				try {

					logger.info("Importing pages");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing pages");

					Files.walkFileTree(pages, new PageImportVisitor(pages, pagesMetadata, relativeVisibility));

				} catch (IOException ioex) {
					logger.warn("Exception while importing pages", ioex);
				}
			}

			// import sites
			if (Files.exists(sitesConfFile)) {

				logger.info("Importing sites");
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing sites");

				importSites(readConfigList(sitesConfFile));
			}

			// link pages
			try (final Tx tx = app.tx()) {

				tx.disableChangelog();

				deferredPageLinks.forEach((String linkableUUID, String pagePath) -> {

					try {
						final DOMNode page        = StructrApp.getInstance().get(DOMNode.class, linkableUUID);
						final Linkable linkedPage = StructrApp.getInstance().nodeQuery(Linkable.class).and(StructrApp.key(Page.class, "path"), pagePath).or(Page.name, pagePath).getFirst();

						((LinkSource)page).setLinkable(linkedPage);

					} catch (FrameworkException ex) {
					}

				});

				deferredPageLinks.clear();

				tx.success();
			}


			// import application data
			final Path dataDir = source.resolve("data");
			if (Files.exists(dataDir) && Files.isDirectory(dataDir)) {

				logger.info("Importing application data");
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing application data");

				final DeployDataCommand cmd = StructrApp.getInstance(securityContext).command(DeployDataCommand.class);

				cmd.doImportFromDirectory(dataDir);
			}


			// apply post-deploy.conf
			applyConfigurationFile(ctx, source.resolve("post-deploy.conf"), DEPLOYMENT_IMPORT_STATUS);

			if (!missingPrincipals.isEmpty()) {

				final String title = "Missing Principal(s)";
				final String text = "The following user(s) and/or group(s) are missing for grants or node ownership during <b>deployment</b>.<br>"
						+ "Because of these missing grants/ownerships, <b>the functionality is not identical to the export you just imported</b>!"
						+ "<ul><li>" + String.join("</li><li>",  missingPrincipals) + "</li></ul>"
						+ "Consider adding these principals to your <a href=\"https://support.structr.com/article/428#pre-deployconf-javascript\">pre-deploy.conf</a> and re-importing.";

				logger.info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tThe following user(s) and/or group(s) are missing for grants or node ownership during deployment.\n"
						+ "\tBecause of these missing grants/ownerships, the functionality is not identical to the export you just imported!\n\n"
						+ "\t" + String.join("\n\t",  missingPrincipals)
						+ "\n\n\tConsider adding these principals to your 'pre-deploy.conf' (see https://support.structr.com/article/428#pre-deployconf-javascript) and re-importing.\n"
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

		} catch (Throwable t) {

			publishWarningMessage("Fatal Error", "Something went wrong - the deployment import has stopped. Please see the log for more information");

			throw t;

		} finally {

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

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("start",  startTime);
			broadcastData.put("target", target.toString());
			publishBeginMessage(DEPLOYMENT_EXPORT_STATUS, broadcastData);

			Files.createDirectories(target);

			final Path components          = Files.createDirectories(target.resolve("components"));
			final Path files               = Files.createDirectories(target.resolve("files"));
			final Path pages               = Files.createDirectories(target.resolve("pages"));
			final Path schemaFolder        = Files.createDirectories(target.resolve("schema"));
			final Path security            = Files.createDirectories(target.resolve("security"));
			final Path templates           = Files.createDirectories(target.resolve("templates"));
			final Path modules             = Files.createDirectories(target.resolve("modules"));
			final Path mailTemplatesFolder = Files.createDirectories(target.resolve("mail-templates"));
			final Path grantsConf          = security.resolve("grants.json");
			final Path filesConf           = target.resolve("files.json");
			final Path sitesConf           = target.resolve("sites.json");
			final Path pagesConf           = target.resolve("pages.json");
			final Path componentsConf      = target.resolve("components.json");
			final Path templatesConf       = target.resolve("templates.json");
			final Path mailTemplatesConf   = target.resolve("mail-templates.json");
			final Path localizationsConf   = target.resolve("localizations.json");
			final Path widgetsConf         = target.resolve("widgets.json");
			final Path deploymentConfFile = target.resolve("deployment.conf");
			final Path applicationConfigurationData = target.resolve("application-configuration-data.json");

			writeDeploymentConfigurationFile(deploymentConfFile);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Files");
			exportFiles(files, filesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Sites");
			exportSites(sitesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Pages");
			exportPages(pages, pagesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Components");
			exportComponents(components, componentsConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Templates");
			exportTemplates(templates, templatesConf);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Resource Access Grants");
			exportResourceAccessGrants(grantsConf);

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


		} catch (IOException ex) {
			logger.warn("", ex);
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
			config.put(folder.getPath(), properties);
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

	private void exportFile(final Path target, final File file, final Map<String, Object> config) throws IOException {

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
			config.put(file.getPath(), properties);
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

	private void exportPages(final Path target, final Path configTarget) throws FrameworkException {

		logger.info("Exporting pages (unchanged pages will be skipped)");

		final Map<String, Object> pagesConfig = new TreeMap<>();
		final App app                         = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final Page page : app.nodeQuery(Page.class).sort(Page.name).getAsList()) {

				if (!(page instanceof ShadowDocument)) {

					final String content = page.getContent(RenderContext.EditMode.DEPLOYMENT);
					if (content != null) {

						final Map<String, Object> properties = new TreeMap<>();
						final String name                    = page.getName();
						final Path pageFile                  = target.resolve(name + ".html");
						boolean doExport                     = true;

						if (Files.exists(pageFile)) {

							try {

								final String existingContent = new String(Files.readAllBytes(pageFile), "utf-8");
								doExport = !existingContent.equals(content);

							} catch (IOException ignore) {
								logger.warn("", ignore);
							}
						}

						pagesConfig.put(name, properties);
						exportConfiguration(page, properties);
						exportOwnershipAndSecurity(page, properties);

						if (doExport) {

							writeStringToFile(pageFile, content);
						}
					}
				}
			}

			tx.success();
		}

		writeJsonToFile(configTarget, pagesConfig);
	}

	private void exportComponents(final Path target, final Path configTarget) throws FrameworkException {

		logger.info("Exporting components (unchanged components will be skipped)");

		final Map<String, Object> configuration = new TreeMap<>();
		final App app                           = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final ShadowDocument shadowDocument = app.nodeQuery(ShadowDocument.class).getFirst();
			if (shadowDocument != null) {

				for (final DOMNode node : shadowDocument.getElements()) {

					final boolean hasParent = node.getParent() != null;
					final boolean inTrash   = node.inTrash();
					boolean doExport        = true;

					// skip nodes in trash and non-toplevel nodes
					if (inTrash || hasParent) {
						continue;
					}

					final String content = node.getContent(RenderContext.EditMode.DEPLOYMENT);
					if (content != null) {

						// name with uuid or just uuid
						String name = node.getProperty(AbstractNode.name);

						if (name != null) {

							name += "-" + node.getUuid();

						} else {

							name = node.getUuid();
						}


						final Map<String, Object> properties = new TreeMap<>();
						final Path targetFile = target.resolve(name + ".html");

						if (Files.exists(targetFile)) {

							try {

								final String existingContent = new String(Files.readAllBytes(targetFile), "utf-8");
								doExport = !existingContent.equals(content);

							} catch (IOException ignore) {}
						}

						configuration.put(name, properties);
						exportConfiguration(node, properties);

						if (doExport) {

							writeStringToFile(targetFile, content);
						}
					}
				}
			}

			tx.success();
		}

		writeJsonToFile(configTarget, configuration);
	}

	private void exportTemplates(final Path target, final Path configTarget) throws FrameworkException {

		logger.info("Exporting templates (unchanged templates will be skipped)");

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

				exportTemplateSource(target, template, configuration);
			}

			tx.success();
		}

		writeJsonToFile(configTarget, configuration);
	}

	private void exportTemplateSource(final Path target, final DOMNode template, final Map<String, Object> configuration) throws FrameworkException {

		final String content                 = template.getProperty(StructrApp.key(Template.class, "content"));
		final Map<String, Object> properties = new TreeMap<>();
		boolean doExport                     = true;

		if (content != null) {

			// name with uuid or just uuid
			String name = template.getProperty(AbstractNode.name);
			if (name != null) {

				name += "-" + template.getUuid();

			} else {

				name = template.getUuid();
			}

			final Path targetFile = target.resolve(name + ".html");
			if (Files.exists(targetFile)) {

				try {

					final String existingContent = new String(Files.readAllBytes(targetFile), "utf-8");
					doExport = !existingContent.equals(content);

				} catch (IOException ignore) {}
			}

			configuration.put(name, properties);
			exportConfiguration(template, properties);

			if (doExport) {

				writeStringToFile(targetFile, content);
			}
		}
	}

	private void exportResourceAccessGrants(final Path target) throws FrameworkException {

		logger.info("Exporting resource access grants");

		final List<Map<String, Object>> grants = new LinkedList<>();
		final App app                          = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess res : app.nodeQuery(ResourceAccess.class).sort(ResourceAccess.signature).getAsList()) {

				final Map<String, Object> grant = new TreeMap<>();
				grants.add(grant);

				grant.put("id",        res.getProperty(ResourceAccess.id));
				grant.put("signature", res.getProperty(ResourceAccess.signature));
				grant.put("flags",     res.getProperty(ResourceAccess.flags));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			final Gson gson                = new GsonBuilder().serializeNulls().create();
			final StringBuilder sb         = new StringBuilder("[");
			final List<String> jsonStrings = new LinkedList();

			for (final Map<String, Object> grant : grants) {

				jsonStrings.add("\t" + gson.toJson(grant));
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

	private void exportSchema(final Path targetFolder) throws FrameworkException {

		logger.info("Exporting schema");

		try {

			// first delete all contents of the schema directory
			deleteDirectoryContentsRecursively(targetFolder);

			final StructrSchemaDefinition schema = (StructrSchemaDefinition)StructrSchema.createFromDatabase(StructrApp.getInstance());

			if (Settings.SchemaDeploymentFormat.getValue().equals("tree")) {

				// move global schema methods to files
				final List<Map<String, Object>> globalSchemaMethods = schema.getGlobalMethods();

				if (globalSchemaMethods.size() > 0) {

					final Path globalMethodsFolder = Files.createDirectories(targetFolder.resolve(DEPLOYMENT_SCHEMA_GLOBAL_METHODS_FOLDER));

					for (Map<String, Object> schemaMethod : globalSchemaMethods) {

						final String methodSource     = (String) schemaMethod.get(DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY);
						final Path globalMethodFile   = globalMethodsFolder.resolve((String) schemaMethod.get("name"));
						final String relativeFilePath = "./" + targetFolder.relativize(globalMethodFile).toString();

						schemaMethod.put(DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY, relativeFilePath);

						if (Files.exists(globalMethodFile)) {
							logger.warn("File '{}' already exists - this can happen if there is a non-unique global method definition. This is not supported in tree-based schema export and will causes errors!", relativeFilePath);
						}

						if (methodSource != null) {
							writeStringToFile(globalMethodFile, methodSource);
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

					final boolean hasFunctionProperties = (functionProperties.size() > 0);
					final boolean hasMethods = (typeDef.getMethods().size() > 0);

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
								final String methodSource            = method.getSource();

								final Path methodFile = methodsFolder.resolve(method.getName());

								if (methodSource != null) {
									writeStringToFile(methodFile, methodSource);
									method.setSource("./" + targetFolder.relativize(methodFile).toString());
								}
							}
						}
					}
				}
			}

			final Path schemaJson = targetFolder.resolve("schema.json");

			writeStringToFile(schemaJson, schema.toString());

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private void exportConfiguration(final DOMNode node, final Map<String, Object> config) throws FrameworkException {

		putData(config, "id",                          node.getProperty(DOMNode.id));
		putData(config, "visibleToPublicUsers",        node.isVisibleToPublicUsers());
		putData(config, "visibleToAuthenticatedUsers", node.isVisibleToAuthenticatedUsers());
		putData(config, "contentType",                 node.getProperty(StructrApp.key(Content.class, "contentType")));

		if (node instanceof Template) {

			// mark this template as being shared
			putData(config, "shared", Boolean.toString(node.isSharedComponent() && node.getParent() == null));
		}

		if (node instanceof Page) {

			putData(config, "path",                    node.getProperty(StructrApp.key(Page.class, "path")));
			putData(config, "position",                node.getProperty(StructrApp.key(Page.class, "position")));
			putData(config, "category",                node.getProperty(StructrApp.key(Page.class, "category")));
			putData(config, "showOnErrorCodes",        node.getProperty(StructrApp.key(Page.class, "showOnErrorCodes")));
			putData(config, "showConditions",          node.getProperty(StructrApp.key(Page.class, "showConditions")));
			putData(config, "hideConditions",          node.getProperty(StructrApp.key(Page.class, "hideConditions")));
			putData(config, "dontCache",               node.getProperty(StructrApp.key(Page.class, "dontCache")));
			putData(config, "cacheForSeconds",         node.getProperty(StructrApp.key(Page.class, "cacheForSeconds")));
			putData(config, "pageCreatesRawData",      node.getProperty(StructrApp.key(Page.class, "pageCreatesRawData")));
			putData(config, "basicAuthRealm",          node.getProperty(StructrApp.key(Page.class, "basicAuthRealm")));
			putData(config, "enableBasicAuth",         node.getProperty(StructrApp.key(Page.class, "enableBasicAuth")));
			putData(config, "hidden",                  node.getProperty(StructrApp.key(Page.class, "hidden")));

		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(node.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (!key.isPartOfBuiltInSchema() && key.relatedType() == null) {

				putData(config, key.jsonName(), node.getProperty(key));
			}
		}
	}

	private void exportFileConfiguration(final AbstractFile abstractFile, final Map<String, Object> config) {

		putData(config, "id",                          abstractFile.getProperty(AbstractFile.id));
		putData(config, "visibleToPublicUsers",        abstractFile.isVisibleToPublicUsers());
		putData(config, "visibleToAuthenticatedUsers", abstractFile.isVisibleToAuthenticatedUsers());

		if (abstractFile instanceof File) {

			final File file = (File)abstractFile;

			putData(config, "isTemplate", file.isTemplate());
			putData(config, "dontCache", abstractFile.getProperty(StructrApp.key(File.class, "dontCache")));
		}

		putData(config, "type",                        abstractFile.getProperty(File.type));
		putData(config, "contentType",                 abstractFile.getProperty(StructrApp.key(File.class, "contentType")));
		putData(config, "cacheForSeconds",             abstractFile.getProperty(StructrApp.key(File.class, "cacheForSeconds")));
		putData(config, "useAsJavascriptLibrary",      abstractFile.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));
		putData(config, "includeInFrontendExport",     abstractFile.getProperty(StructrApp.key(File.class, "includeInFrontendExport")));
		putData(config, "basicAuthRealm",              abstractFile.getProperty(StructrApp.key(File.class, "basicAuthRealm")));
		putData(config, "enableBasicAuth",             abstractFile.getProperty(StructrApp.key(File.class, "enableBasicAuth")));

		if (abstractFile instanceof Image) {

			final Image image = (Image)abstractFile;

			putData(config, "isThumbnail",             image.isThumbnail());
			putData(config, "isImage",                 image.isImage());
			putData(config, "width",                   image.getWidth());
			putData(config, "height",                  image.getHeight());
		}

		if (abstractFile instanceof AbstractMinifiedFile) {

			if (abstractFile instanceof MinifiedCssFile) {

				final MinifiedCssFile mcf = (MinifiedCssFile)abstractFile;

				putData(config, "lineBreak", mcf.getLineBreak());
			}

			if (abstractFile instanceof MinifiedJavaScriptFile) {

				final MinifiedJavaScriptFile mjf = (MinifiedJavaScriptFile)abstractFile;

				putData(config, "optimizationLevel", mjf.getOptimizationLevel());
			}

			final Class<Relation> relType                  = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
			final PropertyKey<Integer> positionKey         = StructrApp.key(relType, "position");
			final Map<Integer, String> minificationSources = new TreeMap<>();

			for(Relation minificationSourceRel : AbstractMinifiedFile.getSortedMinificationRelationships((AbstractMinifiedFile)abstractFile)) {

				final File file = (File) minificationSourceRel.getTargetNode();

				minificationSources.put(minificationSourceRel.getProperty(positionKey), file.getPath());
			}

			putData(config, "minificationSources", minificationSources);
		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(abstractFile.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (!key.isPartOfBuiltInSchema() && key.relatedType() == null) {

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

	protected void checkOwnerAndSecurity(final Map<String, Object> entry) throws FrameworkException {

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

			} else {
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

			// first delete all contents of the schema directory
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
			t.printStackTrace();
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
				putData(entry, "pictures",                    widget.getProperty(StructrApp.key(Widget.class, "pictures")));
				putData(entry, "configuration",               widget.getProperty(StructrApp.key(Widget.class, "configuration")));
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

		writeJsonToFile(target, applicationConfigurationDataNodes);
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

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			localizations.sort(new AbstractMapComparator<Object>() {
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

			final Gson gson = new GsonBuilder().serializeNulls().create();

			final StringBuilder sb = new StringBuilder("[");

			List<String> jsonStrings = new LinkedList();

			for (Map<String, Object> loc : localizations) {
				jsonStrings.add("\t" + gson.toJson(loc));
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

	protected void applyConfigurationFile(final SecurityContext ctx, final Path confFile, final String progressType) {

		if (Files.exists(confFile)) {

			final App app = StructrApp.getInstance(ctx);

			try (final Tx tx = app.tx()) {

				tx.disableChangelog();

				final String confSource = new String(Files.readAllBytes(confFile), Charset.forName("utf-8")).trim();

				if (confSource.length() > 0) {

					final String message = "Applying configuration from '" + confFile + "'";
					logger.info(message);
					publishProgressMessage(progressType, message);

					Scripting.evaluate(new ActionContext(ctx), null, confSource, confFile.getFileName().toString());

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

			logger.error("Unable to import {}, aborting with {}", type.getSimpleName(), fex.getMessage());
			fex.printStackTrace();

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

			logger.error("Unable to import site, aborting with {}", fex.getMessage());
			fex.printStackTrace();

			throw fex;
		}
	}

	private void importSchema(final Path schemaFolder, final boolean extendExistingSchema) {

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

							final Path globalMethodFile = globalMethodsFolder.resolve((String) schemaMethod.get("name"));

							schemaMethod.put(DEPLOYMENT_SCHEMA_SOURCE_ATTRIBUTE_KEY, (Files.exists(globalMethodFile)) ? new String(Files.readAllBytes(globalMethodFile)) : null);
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

									if (method.getSource() != null) {

										final Path methodSourceFile = methodsFolder.resolve(method.getName());

										if (Files.exists(methodSourceFile)) {
											method.setSource(new String(Files.readAllBytes(methodSourceFile)));
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

				if (extendExistingSchema) {

					StructrSchema.extendDatabaseSchema(app, schema);

				} else {

					StructrSchema.replaceDatabaseSchema(app, schema);
				}

			} catch (Throwable t) {

				throw new ImportFailureException(t.getMessage(), t);
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

	private void writeStringToFile(final Path path, final String string) {

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


	public static void main(final String[] args) {

		System.out.println(Float.valueOf("3.5-SNAPSHPOT"));
		System.out.println(Float.valueOf("3.4.3"));
	}
}
