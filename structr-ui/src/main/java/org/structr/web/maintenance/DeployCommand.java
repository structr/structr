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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
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
import org.structr.schema.SchemaHelper;
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

/**
 *
 */
public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger                     = LoggerFactory.getLogger(DeployCommand.class.getName());
	private static final Pattern pattern                   = Pattern.compile("[a-f0-9]{32}");

	private static final Map<String, String> deferredPageLinks = new LinkedHashMap<>();
	protected static final Set<String> missingPrincipals       = new HashSet<>();

	private final static String DEPLOYMENT_IMPORT_STATUS   = "DEPLOYMENT_IMPORT_STATUS";
	private final static String DEPLOYMENT_EXPORT_STATUS   = "DEPLOYMENT_EXPORT_STATUS";

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("deploy", DeployCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> parameters) throws FrameworkException {

		final String mode = (String) parameters.get("mode");

		if ("export".equals(mode)) {

			doExport(parameters);

		} else if ("import".equals(mode)) {

			doImport(parameters);

		} else {

			warn("Unsupported mode '{}'", mode);
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

	public Map<String, Object> readConfigMap(final Path pagesConf) {

		if (Files.exists(pagesConf)) {

			try (final Reader reader = Files.newBufferedReader(pagesConf, Charset.forName("utf-8"))) {

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
		return new GsonBuilder().setPrettyPrinting().setDateFormat(Settings.DefaultDateFormat.getValue()).create();
	}

	public static boolean isUuid(final String name) {
		return pattern.matcher(name).matches();
	}

	/**
	 * Checks if the given string ends with a uuid
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

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final String path         = (String) attributes.get("source");
			final SecurityContext ctx = SecurityContext.getSuperUserInstance();
			final App app             = StructrApp.getInstance(ctx);

			ctx.setDoTransactionNotifications(false);
			ctx.disableEnsureCardinality();
			ctx.disableModificationOfAccessTime();

			final Map<String, Object> componentsConf = new HashMap<>();
			final Map<String, Object> templatesConf  = new HashMap<>();
			final Map<String, Object> pagesConf      = new HashMap<>();
			final Map<String, Object> filesConf      = new HashMap<>();

			if (StringUtils.isBlank(path)) {

				throw new FrameworkException(422, "Please provide 'source' attribute for deployment source directory path.");
			}

			final Path source = Paths.get(path);
			if (!Files.exists(source)) {

				throw new FrameworkException(422, "Source path " + path + " does not exist.");
			}

			if (!Files.isDirectory(source)) {

				throw new FrameworkException(422, "Source path " + path + " is not a directory.");
			}

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("start",   startTime);
			broadcastData.put("source",  source.toString());
			publishBeginMessage(DEPLOYMENT_IMPORT_STATUS, broadcastData);

			// apply configuration
			final Path preDeployConf = source.resolve("pre-deploy.conf");
			if (Files.exists(preDeployConf)) {

				try (final Tx tx = app.tx()) {

					tx.disableChangelog();

					final String confSource = new String(Files.readAllBytes(preDeployConf), Charset.forName("utf-8")).trim();

					if (confSource.length() > 0) {

						info("Applying pre-deployment configuration from {}", preDeployConf);
						publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Applying pre-deployment configuration");

						Scripting.evaluate(new ActionContext(ctx), null, confSource, "pre-deploy.conf");

					} else {

						info("Ignoring empty pre-deployment configuration {}", preDeployConf);
					}

					tx.success();

				} catch (Throwable t) {

					final String msg = "Exception caught while importing pre-deploy.conf";
					logger.warn(msg, t);
					publishWarningMessage(msg, t.toString());
				}
			}

			// read grants.json
			final Path grantsConf = source.resolve("security/grants.json");
			if (Files.exists(grantsConf)) {

				info("Reading {}", grantsConf);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing resource access grants");

				importListData(ResourceAccess.class, readConfigList(grantsConf));
			}

			// read schema-methods.json
			final Path schemaMethodsConf = source.resolve("schema-methods.json");
			if (Files.exists(schemaMethodsConf)) {

				info("Reading {}", schemaMethodsConf);
				final String title = "Deprecation warning";
				final String text = "Found file 'schema-methods.json'. Newer versions store global schema methods in the schema snapshot file. Recreate the export with the current version to avoid compatibility issues. Support for importing this file will be dropped in future versions.";

				info(title + ": " + text);
				publishWarningMessage(title, text);

				importListData(SchemaMethod.class, readConfigList(schemaMethodsConf));
			}

			// read mail-templates.json
			final Path mailTemplatesConf = source.resolve("mail-templates.json");
			if (Files.exists(mailTemplatesConf)) {

				info("Reading {}", mailTemplatesConf);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing mail templates");

				importListData(MailTemplate.class, readConfigList(mailTemplatesConf));
			}

			// read widgets.json
			final Path widgetsConf = source.resolve("widgets.json");
			if (Files.exists(widgetsConf)) {

				info("Reading {}", widgetsConf);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing widgets");

				importListData(Widget.class, readConfigList(widgetsConf));
			}

			// read localizations.json
			final Path localizationsConf = source.resolve("localizations.json");
			if (Files.exists(localizationsConf)) {

				final PropertyMap additionalData = new PropertyMap();

				// Question: shouldn't this be true? No, 'imported' is a flag for legacy-localization which
				// have been imported from a legacy-system which was replaced by structr.
				// it is a way to differentiate between new and old localization strings
				additionalData.put(StructrApp.key(Localization.class, "imported"), false);

				info("Reading {}", localizationsConf);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing localizations");

				importListData(Localization.class, readConfigList(localizationsConf), additionalData);
			}

			// read widgets.json
			final Path applicationConfigurationDataConf = source.resolve("application-configuration-data.json");
			if (Files.exists(applicationConfigurationDataConf)) {

				info("Reading {}", applicationConfigurationDataConf);
				publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing application configuration data");

				importListData(ApplicationConfigurationDataNode.class, readConfigList(applicationConfigurationDataConf));
			}

			// read files.conf
			final Path filesConfFile = source.resolve("files.json");
			if (Files.exists(filesConfFile)) {

				info("Reading {}", filesConfFile);
				filesConf.putAll(readConfigMap(filesConfFile));
			}

			// read pages.conf
			final Path pagesConfFile = source.resolve("pages.json");
			if (Files.exists(pagesConfFile)) {

				info("Reading {}", pagesConfFile);
				pagesConf.putAll(readConfigMap(pagesConfFile));
			}

			// read components.conf
			final Path componentsConfFile = source.resolve("components.json");
			if (Files.exists(componentsConfFile)) {

				info("Reading {}", componentsConfFile);
				componentsConf.putAll(readConfigMap(componentsConfFile));
			}

			// read templates.conf
			final Path templatesConfFile = source.resolve("templates.json");
			if (Files.exists(templatesConfFile)) {

				info("Reading {}", templatesConfFile);
				templatesConf.putAll(readConfigMap(templatesConfFile));
			}

			// import schema
			final Path schemaFolder = source.resolve("schema");
			if (Files.exists(schemaFolder)) {

				try {

					info("Importing data from schema/ directory");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing schema");

					importSchema(schemaFolder);

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

					info("Importing files (unchanged files will be skipped)");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing files");

					FileImportVisitor fiv = new FileImportVisitor(files, filesConf);
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

						info("Importing deployment data for module {}", module.getName());
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
			if (Files.exists(templates) && Files.exists(components) && Files.exists(pages)) {

				try (final Tx tx = app.tx()) {

					tx.disableChangelog();

					info("Removing pages, templates and components");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing pages, templates and components");

					app.delete(DOMNode.class);

					if (Files.exists(sitesConfFile)) {

						info("Removing sites");
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

					info("Importing templates");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing templates");

					Files.walkFileTree(templates, new TemplateImportVisitor(templatesConf));

				} catch (IOException ioex) {
					logger.warn("Exception while importing templates", ioex);
				}
			}

			// make sure shadow document is created in any case
			CreateComponentCommand.getOrCreateHiddenDocument();

			// import components, must be done before pages so the shared components exist
			if (Files.exists(components)) {

				try {

					info("Importing shared components");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing shared components");

					Files.walkFileTree(components, new ComponentImportVisitor(componentsConf));

				} catch (IOException ioex) {
					logger.warn("Exception while importing shared components", ioex);
				}
			}

			// import pages
			if (Files.exists(pages)) {

				try {

					info("Importing pages");
					publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing pages");

					Files.walkFileTree(pages, new PageImportVisitor(pages, pagesConf));

				} catch (IOException ioex) {
					logger.warn("Exception while importing pages", ioex);
				}
			}

			// import sites
			if (Files.exists(sitesConfFile)) {

				info("Importing sites");
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

			// apply configuration
			final Path postDeployConf = source.resolve("post-deploy.conf");
			if (Files.exists(postDeployConf)) {

				try (final Tx tx = app.tx()) {

					tx.disableChangelog();

					final String confSource = new String(Files.readAllBytes(postDeployConf), Charset.forName("utf-8")).trim();

					if (confSource.length() > 0) {

						info("Applying post-deployment configuration from {}", postDeployConf);
						publishProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Applying post-deployment configuration");

						Scripting.evaluate(new ActionContext(ctx), null, confSource, "post-deploy.conf");

					} else {

						info("Ignoring empty post-deployment configuration {}", postDeployConf);

					}

					tx.success();

				} catch (Throwable t) {
					logger.warn("", t);
					publishWarningMessage("Exception caught while importing post-deploy.conf", t.toString());
				}
			}

			if (!missingPrincipals.isEmpty()) {

				final String title = "Missing Principal(s)";
				final String text = "The following user(s) and/or group(s) are missing for grants or node ownership during deployment.<br>"
						+ "Because of these missing grants/ownerships, the functionality is not identical to the export you just imported!<br><br>"
						+ String.join(", ",  missingPrincipals)
						+ "<br><br>Consider adding these principals to your <a href=\"https://support.structr.com/article/428#pre-deployconf-javascript\">pre-deploy.conf</a> and re-importing.";

				info("\n###############################################################################\n"
						+ "\tWarning: " + title + "!\n"
						+ "\tThe following user(s) and/or group(s) are missing for grants or node ownership during deployment.\n"
						+ "\tBecause of these missing grants/ownerships, the functionality is not identical to the export you just imported!\n\n"
						+ "\t" + String.join(", ",  missingPrincipals)
						+ "\n\n\tConsider adding these principals to your 'pre-deploy.conf' (see https://support.structr.com/article/428#pre-deployconf-javascript) and re-importing.\n"
						+ "###############################################################################"
				);
				publishWarningMessage(title, text);
			}

			final long endTime = System.currentTimeMillis();
			DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

			customHeaders.put("end", new Date(endTime).toString());
			customHeaders.put("duration", duration);

			info("Import from {} done. (Took {})", source.toString(), duration);

			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			publishEndMessage(DEPLOYMENT_IMPORT_STATUS, broadcastData);

		} finally {

			// restore saved value
			Settings.ChangelogEnabled.setValue(changeLogEnabled);
		}
	}

	protected void doExport(final Map<String, Object> attributes) throws FrameworkException {

		final String path  = (String) attributes.get("target");

		if (StringUtils.isBlank(path)) {

			throw new FrameworkException(422, "Please provide target path for deployment export.");
		}

		final Path target  = Paths.get(path);

		try {

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("start",  startTime);
			broadcastData.put("target", target.toString());
			publishBeginMessage(DEPLOYMENT_EXPORT_STATUS, broadcastData);

			Files.createDirectories(target);

			final Path components     = Files.createDirectories(target.resolve("components"));
			final Path files          = Files.createDirectories(target.resolve("files"));
			final Path pages          = Files.createDirectories(target.resolve("pages"));
			final Path schemaFolder   = Files.createDirectories(target.resolve("schema"));
			final Path security       = Files.createDirectories(target.resolve("security"));
			final Path templates      = Files.createDirectories(target.resolve("templates"));
			final Path modules        = Files.createDirectories(target.resolve("modules"));
			final Path grants         = security.resolve("grants.json");
			final Path filesConf      = target.resolve("files.json");
			final Path sitesConf      = target.resolve("sites.json");
			final Path pagesConf      = target.resolve("pages.json");
			final Path componentsConf = target.resolve("components.json");
			final Path templatesConf  = target.resolve("templates.json");
			final Path mailTemplates  = target.resolve("mail-templates.json");
			final Path localizations  = target.resolve("localizations.json");
			final Path widgets        = target.resolve("widgets.json");

			final Path applicationConfigurationData = target.resolve("application-configuration-data.json");

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
			exportResourceAccessGrants(grants);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Schema");
			exportSchema(schemaFolder);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Mail Templates");
			exportMailTemplates(mailTemplates);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Localizations");
			exportLocalizations(localizations);

			publishProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Widgets");
			exportWidgets(widgets);

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

			info("Export to {} done. (Took {})", target.toString(), duration);

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

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(configTarget.toFile()))) {

			getGson().toJson(config, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
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
		Collections.sort(folders, new GraphObjectComparator(AbstractNode.name, false));

		for (final Folder child : folders) {
			exportFilesAndFolders(path, child, config);
		}

		final List<File> files = Iterables.toList(folder.getFiles());
		Collections.sort(files, new GraphObjectComparator(AbstractNode.name, false));

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

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(sites, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
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

							try (final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(pageFile.toFile()))) {

								writer.write(content);
								writer.flush();
								writer.close();

							} catch (IOException ioex) {
								logger.warn("", ioex);
							}
						}
					}
				}
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(configTarget.toFile()))) {

			getGson().toJson(pagesConfig, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
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

							try (final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile.toFile()))) {

								writer.write(content);
								writer.flush();
								writer.close();

							} catch (IOException ioex) {
								logger.warn("", ioex);
							}
						}
					}
				}
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(configTarget.toFile()))) {

			getGson().toJson(configuration, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
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

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(configTarget.toFile()))) {

			getGson().toJson(configuration, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void exportTemplateSource(final Path target, final DOMNode template, final Map<String, Object> configuration) throws FrameworkException {

		final Map<String, Object> properties = new TreeMap<>();
		boolean doExport                     = true;

		final String content = template.getProperty(StructrApp.key(Template.class, "content"));
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

				try (final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile.toFile()))) {

					writer.write(content);
					writer.flush();
					writer.close();

				} catch (IOException ioex) {
					logger.warn("", ioex);
				}
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

			final Gson gson = new GsonBuilder().serializeNulls().create();

			final StringBuilder sb = new StringBuilder("[");

			List<String> jsonStrings = new LinkedList();

			for (Map<String, Object> grant : grants) {
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

					final Path globalMethodsFolder = Files.createDirectories(targetFolder.resolve("_globalMethods"));

					for (Map<String, Object> schemaMethod : globalSchemaMethods) {

						final String methodSource     = (String) schemaMethod.get("source");
						final Path globalMethodFile   = globalMethodsFolder.resolve((String) schemaMethod.get("name"));
						final String relativeFilePath = "./" + targetFolder.relativize(globalMethodFile).toString();

						schemaMethod.put("source", relativeFilePath);

						if (Files.exists(globalMethodFile)) {
							logger.warn("File '{}' already exists - this can happen if there is a non-unique global method definition. This is not supported in tree-based schema export and will causes errors!", relativeFilePath);
						}

						if (methodSource != null) {
							writeStringToFile(globalMethodFile, methodSource);
						}
					}
				}

				// move all methods/function properties to files
				for (final StructrTypeDefinition typeDef : schema.getTypes()) {

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

							final Path functionsFolder = Files.createDirectories(typeFolder.resolve("functions"));

							for (final StructrFunctionProperty fp : functionProperties) {

								final Path readFunctionFile  = functionsFolder.resolve(fp.getName() + ".readFunction");
								final String readFunction    = fp.getReadFunction();
								fp.setReadFunction("./" + targetFolder.relativize(readFunctionFile).toString());
								if (readFunction != null) {
									writeStringToFile(readFunctionFile, readFunction);
								}

								final Path writeFunctionFile = functionsFolder.resolve(fp.getName() + ".writeFunction");
								final String writeFunction   = fp.getWriteFunction();
								fp.setWriteFunction("./" + targetFolder.relativize(writeFunctionFile).toString());
								if (writeFunction != null) {
									writeStringToFile(writeFunctionFile, writeFunction);
								}
							}
						}

						if (hasMethods) {

							final Path methodsFolder = Files.createDirectories(typeFolder.resolve("methods"));

							for (final Object m : typeDef.getMethods()) {

								final StructrMethodDefinition method = (StructrMethodDefinition)m;
								final String methodSource = method.getSource();

								final Path methodFile = methodsFolder.resolve(method.getName());
								method.setSource("./" + targetFolder.relativize(methodFile).toString());

								if (methodSource != null) {
									writeStringToFile(methodFile, methodSource);
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

		putIfNotNull(config, "id", node.getProperty(DOMNode.id));

		if (node.isVisibleToPublicUsers())        { putIfNotNull(config, "visibleToPublicUsers", true); }
		if (node.isVisibleToAuthenticatedUsers()) { putIfNotNull(config, "visibleToAuthenticatedUsers", true); }

		putIfNotNull(config, "contentType",             node.getProperty(StructrApp.key(Content.class, "contentType")));

		if (node instanceof Template) {

			// mark this template as being shared
			putIfNotNull(config, "shared", Boolean.toString(node.isSharedComponent() && node.getParent() == null));
		}

		if (node instanceof Page) {

			putIfNotNull(config, "path",                    node.getProperty(StructrApp.key(Page.class, "path")));
			putIfNotNull(config, "position",                node.getProperty(StructrApp.key(Page.class, "position")));
			putIfNotNull(config, "category",                node.getProperty(StructrApp.key(Page.class, "category")));
			putIfNotNull(config, "showOnErrorCodes",        node.getProperty(StructrApp.key(Page.class, "showOnErrorCodes")));
			putIfNotNull(config, "showConditions",          node.getProperty(StructrApp.key(Page.class, "showConditions")));
			putIfNotNull(config, "hideConditions",          node.getProperty(StructrApp.key(Page.class, "hideConditions")));
			putIfNotNull(config, "dontCache",               node.getProperty(StructrApp.key(Page.class, "dontCache")));
			putIfNotNull(config, "cacheForSeconds",         node.getProperty(StructrApp.key(Page.class, "cacheForSeconds")));
			putIfNotNull(config, "pageCreatesRawData",      node.getProperty(StructrApp.key(Page.class, "pageCreatesRawData")));
			putIfNotNull(config, "basicAuthRealm",          node.getProperty(StructrApp.key(Page.class, "basicAuthRealm")));
			putIfNotNull(config, "enableBasicAuth",         node.getProperty(StructrApp.key(Page.class, "enableBasicAuth")));
			putIfTrue   (config, "hidden",                  node.getProperty(StructrApp.key(Page.class, "hidden")));

		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(node.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (!key.isPartOfBuiltInSchema() && key.relatedType() == null) {

				putIfNotNull(config, key.jsonName(), node.getProperty(key));
			}
		}
	}

	private void exportFileConfiguration(final AbstractFile abstractFile, final Map<String, Object> config) {

		putIfNotNull(config, "id", abstractFile.getProperty(AbstractFile.id));

		if (abstractFile.isVisibleToPublicUsers())         { putIfNotNull(config, "visibleToPublicUsers", true); }
		if (abstractFile.isVisibleToAuthenticatedUsers())  { putIfNotNull(config, "visibleToAuthenticatedUsers", true); }

		if (abstractFile instanceof File) {

			final File file = (File)abstractFile;

			if (file.isTemplate())                     { putIfNotNull(config, "isTemplate", true); }

			final boolean dontCache = abstractFile.getProperty(StructrApp.key(File.class, "dontCache"));
			if (dontCache) {
				putIfNotNull(config, "dontCache", dontCache);
			}
		}

		putIfNotNull(config, "type",                        abstractFile.getProperty(File.type));
		putIfNotNull(config, "contentType",                 abstractFile.getProperty(StructrApp.key(File.class, "contentType")));
		putIfNotNull(config, "cacheForSeconds",             abstractFile.getProperty(StructrApp.key(File.class, "cacheForSeconds")));
		putIfNotNull(config, "useAsJavascriptLibrary",      abstractFile.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));
		putIfNotNull(config, "includeInFrontendExport",     abstractFile.getProperty(StructrApp.key(File.class, "includeInFrontendExport")));
		putIfNotNull(config, "basicAuthRealm",              abstractFile.getProperty(StructrApp.key(File.class, "basicAuthRealm")));
		putIfNotNull(config, "enableBasicAuth",             abstractFile.getProperty(StructrApp.key(File.class, "enableBasicAuth")));

		if (abstractFile instanceof Image) {

			final Image image = (Image)abstractFile;

			putIfNotNull(config, "isThumbnail",             image.isThumbnail());
			putIfNotNull(config, "isImage",                 image.isImage());
			putIfNotNull(config, "width",                   image.getWidth());
			putIfNotNull(config, "height",                  image.getHeight());
		}

		if (abstractFile instanceof AbstractMinifiedFile) {

			if (abstractFile instanceof MinifiedCssFile) {

				final MinifiedCssFile mcf = (MinifiedCssFile)abstractFile;

				putIfNotNull(config, "lineBreak", mcf.getLineBreak());
			}

			if (abstractFile instanceof MinifiedJavaScriptFile) {

				final MinifiedJavaScriptFile mjf = (MinifiedJavaScriptFile)abstractFile;

				putIfNotNull(config, "optimizationLevel", mjf.getOptimizationLevel());
			}

			final Class<Relation> relType                  = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
			final PropertyKey<Integer> positionKey         = StructrApp.key(relType, "position");
			final Map<Integer, String> minificationSources = new TreeMap<>();

			for(Relation minificationSourceRel : AbstractMinifiedFile.getSortedRelationships((AbstractMinifiedFile)abstractFile)) {

				final File file = (File) minificationSourceRel.getTargetNode();

				minificationSources.put(minificationSourceRel.getProperty(positionKey), file.getPath());
			}
			putIfNotNull(config, "minificationSources", minificationSources);

		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(abstractFile.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys that are *not* remote properties
			if (!key.isPartOfBuiltInSchema() && key.relatedType() == null) {

				putIfNotNull(config, key.jsonName(), abstractFile.getProperty(key));
			}
		}

		exportOwnershipAndSecurity(abstractFile, config);
	}

	protected void exportOwnershipAndSecurity(final NodeInterface node, final Map<String, Object> config) {

		// export owner
		final Principal owner = node.getOwnerNode();
		if (owner != null) {

			final Map<String, Object> map = new HashMap<>();
			map.put("name", owner.getName());

			config.put("owner", map);
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

		// export non-empty collection only
		if (!grantees.isEmpty()) {
			config.put("grantees", grantees);
		}
	}

	private void checkOwnerAndSecurity(final Map<String, Object> entry) throws FrameworkException {

		if (entry.containsKey("owner")) {

			final String ownerName = (String) ((Map)entry.get("owner")).get("name");
			final Principal owner = StructrApp.getInstance().nodeQuery(Principal.class).andName(ownerName).getFirst();

			if (owner == null) {
				logger.warn("Unknown owner {}, ignoring.", ownerName);
				DeployCommand.addMissingPrincipal(ownerName);

				entry.remove("owner");
			}
		}

		if (entry.containsKey("grantees")) {

			final List<Map<String, Object>> grantees = (List) entry.get("grantees");

			final List<Map<String, Object>> cleanedGrantees = new LinkedList();

			for (final Map<String, Object> grantee : grantees) {

				final String granteeName = (String) grantee.get("name");
				final Principal owner = StructrApp.getInstance().nodeQuery(Principal.class).andName(granteeName).getFirst();

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

	private void exportMailTemplates(final Path target) throws FrameworkException {

		logger.info("Exporting mail templates");

		final PropertyKey<String> textKey             = StructrApp.key(MailTemplate.class, "text");
		final PropertyKey<String> localeKey           = StructrApp.key(MailTemplate.class, "locale");
		final List<Map<String, Object>> mailTemplates = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final MailTemplate mailTemplate : app.nodeQuery(MailTemplate.class).sort(MailTemplate.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				mailTemplates.add(entry);

				putIfNotNull(entry, "id",                          mailTemplate.getProperty(MailTemplate.id));
				putIfNotNull(entry, "name",                        mailTemplate.getProperty(MailTemplate.name));
				putIfNotNull(entry, "text",                        mailTemplate.getProperty(textKey));
				putIfNotNull(entry, "locale",                      mailTemplate.getProperty(localeKey));
				putIfNotNull(entry, "visibleToAuthenticatedUsers", mailTemplate.getProperty(MailTemplate.visibleToAuthenticatedUsers));
				putIfNotNull(entry, "visibleToPublicUsers",        mailTemplate.getProperty(MailTemplate.visibleToPublicUsers));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			mailTemplates.sort(new AbstractMapComparator<Object>() {
				@Override
				public String getKey (Map<String, Object> map) {
					return ((String)map.get("name")).concat(((String)map.get("locale")));
				}
			});

			getGson().toJson(mailTemplates, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void exportWidgets(final Path target) throws FrameworkException {

		logger.info("Exporting widgets");

		final List<Map<String, Object>> widgets = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final Widget widget : app.nodeQuery(Widget.class).sort(Widget.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				widgets.add(entry);

				putIfNotNull(entry, "id",                          widget.getProperty(Widget.id));
				putIfNotNull(entry, "name",                        widget.getProperty(Widget.name));
				putIfNotNull(entry, "visibleToAuthenticatedUsers", widget.getProperty(Widget.visibleToAuthenticatedUsers));
				putIfNotNull(entry, "visibleToPublicUsers",        widget.getProperty(Widget.visibleToPublicUsers));
				putIfNotNull(entry, "source",                      widget.getProperty(StructrApp.key(Widget.class, "source")));
				putIfNotNull(entry, "description",                 widget.getProperty(StructrApp.key(Widget.class, "description")));
				putIfNotNull(entry, "isWidget",                    widget.getProperty(StructrApp.key(Widget.class, "isWidget")));
				putIfNotNull(entry, "treePath",                    widget.getProperty(StructrApp.key(Widget.class, "treePath")));
				putIfNotNull(entry, "pictures",                    widget.getProperty(StructrApp.key(Widget.class, "pictures")));
				putIfNotNull(entry, "configuration",               widget.getProperty(StructrApp.key(Widget.class, "configuration")));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(widgets, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
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

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(applicationConfigurationDataNodes, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
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

					// null domain is replaced by a string so that those localizations are shown first
					return (name != null ? name.toString() : "null").concat((domain != null ? domain.toString() : "00-nulldomain")).concat((locale != null ? locale.toString() : "null"));
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

	protected void putIfNotNull(final Map<String, Object> target, final String key, final Object value) {

		if (value != null) {

			if (value instanceof Iterable) {

				final List list = Iterables.toList((Iterable)value);
				if (!list.isEmpty()) {

					target.put(key, list);
				}

			} else {

				target.put(key, value);
			}
		}
	}

	private void putIfTrue(final Map<String, Object> target, final String key, final Object value) {

		if (Boolean.TRUE.equals(value)) {

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

	protected <T extends NodeInterface> void importExtensibleNodeListData(final String defaultTypeName, final List<Map<String, Object>> data, final PropertyMap... additionalData) throws FrameworkException {

		final Class defaultType = SchemaHelper.getEntityClassForRawType(defaultTypeName);

		if (defaultType == null) {
			throw new FrameworkException(422, "Type cannot be found: " + defaultTypeName);
		}

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		context.setDoTransactionNotifications(false);
		final App app                 = StructrApp.getInstance(context);

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
				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, type, entry);

				// allow caller to insert additional data for better creation performance
				for (final PropertyMap add : additionalData) {
					map.putAll(add);
				}

				app.create(type, map);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error("Unable to import {}, aborting with {}", defaultType.getSimpleName(), fex.getMessage());
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

	private void importSchema(final Path schemaFolder) {

		final Path schemaJsonFile = schemaFolder.resolve("schema.json");

		if (!Files.exists(schemaJsonFile)) {

			info("Deployment does not contain schema/schema.json - continuing without schema import");

		} else {

			final SecurityContext ctx = SecurityContext.getSuperUserInstance();
			ctx.setDoTransactionNotifications(false);

			final App app = StructrApp.getInstance(ctx);

			try (final FileReader reader = new FileReader(schemaJsonFile.toFile())) {

				final StructrSchemaDefinition schema = (StructrSchemaDefinition)StructrSchema.createFromSource(reader);


				if (Settings.SchemaDeploymentFormat.getValue().equals("tree")) {

					final Path globalMethodsFolder = schemaFolder.resolve("_globalMethods");

					if (Files.exists(globalMethodsFolder)) {

						for (Map<String, Object> schemaMethod : schema.getGlobalMethods()) {

							final Path globalMethodFile = globalMethodsFolder.resolve((String) schemaMethod.get("name"));

							schemaMethod.put("source", (Files.exists(globalMethodFile)) ? new String(Files.readAllBytes(globalMethodFile)) : null);
						}

					} else {
						// looks like an old export - dont touch
					}

					for (final StructrTypeDefinition typeDef : schema.getTypes()) {

						final Path typeFolder = schemaFolder.resolve(typeDef.getName());

						if (Files.exists(typeFolder)) {

							final Path functionsFolder = typeFolder.resolve("functions");

							if (Files.exists(functionsFolder)) {

								for (final Object propDef : typeDef.getProperties()) {

									if (propDef instanceof StructrFunctionProperty) {

										final StructrFunctionProperty fp = (StructrFunctionProperty)propDef;

										final Path readFunctionFile    = functionsFolder.resolve(fp.getName() + ".readFunction");
										final Path writeFunctionFile   = functionsFolder.resolve(fp.getName() + ".writeFunction");

										fp.setReadFunction ((Files.exists(readFunctionFile))  ? new String(Files.readAllBytes(readFunctionFile))  : null);
										fp.setWriteFunction((Files.exists(writeFunctionFile)) ? new String(Files.readAllBytes(writeFunctionFile)) : null);
									}
								}
							}

							final Path methodsFolder = typeFolder.resolve("methods");

							if (Files.exists(methodsFolder)) {

								for (final Object m : typeDef.getMethods()) {

									final StructrMethodDefinition method = (StructrMethodDefinition)m;
									final Path methodFile = methodsFolder.resolve(method.getName());

									method.setSource((Files.exists(methodFile)) ? new String(Files.readAllBytes(methodFile)) : null);
								}
							}

						} else {
							// looks like an old export - dont touch
						}
					}
				}

				StructrSchema.replaceDatabaseSchema(app, schema);

			} catch (Throwable t) {

				throw new ImportFailureException(t.getMessage(), t);
			}
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
				writer.append(string);
			}
			writer.flush();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	// ----- public static methods -----
	public static void addDeferredPagelink (String linkableUUID, String pagePath) {
		deferredPageLinks.put(linkableUUID, pagePath);
	}

	public static void addMissingPrincipal (final String principalName) {
		missingPrincipals.add(principalName);
	}

	// ----- nested helper classes -----
	protected static class IdFirstComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			if ("id".equals(o1)) {
				return -1;
			}
			if ("id".equals(o2)) {
				return 1;
			}
			return o1.compareTo(o2);
		}
	}

}
