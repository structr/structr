/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Localization;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.Security;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.module.StructrModule;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.AbstractMinifiedFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.MinifiedCssFile;
import org.structr.web.entity.MinifiedJavaScriptFile;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.relation.ResourceLink;
import org.structr.web.entity.relation.FileChildren;
import org.structr.web.entity.relation.FileSiblings;
import org.structr.web.entity.relation.FolderChildren;
import org.structr.web.entity.relation.Folders;
import org.structr.web.entity.relation.Images;
import org.structr.web.entity.relation.MinificationSource;
import org.structr.web.entity.relation.Thumbnails;
import org.structr.web.entity.relation.UserFavoriteFavoritable;
import org.structr.web.entity.relation.UserFavoriteFile;
import org.structr.web.entity.relation.UserWorkDir;
import org.structr.web.maintenance.deploy.ComponentImportVisitor;
import org.structr.web.maintenance.deploy.FileImportVisitor;
import org.structr.web.maintenance.deploy.PageImportVisitor;
import org.structr.web.maintenance.deploy.SchemaImportVisitor;
import org.structr.web.maintenance.deploy.TemplateImportVisitor;

/**
 *
 */
public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger                     = LoggerFactory.getLogger(DeployCommand.class.getName());
	private static final Pattern pattern                   = Pattern.compile("[a-f0-9]{32}");

	private static final Map<String, String> deferredPageLinks = new LinkedHashMap<>();

	private Integer stepCounter                            = 0;
	private final static String DEPLOYMENT_IMPORT_STATUS   = "DEPLOYMENT_IMPORT_STATUS";
	private final static String DEPLOYMENT_EXPORT_STATUS   = "DEPLOYMENT_EXPORT_STATUS";
	private final static String DEPLOYMENT_STATUS_BEGIN    = "BEGIN";
	private final static String DEPLOYMENT_STATUS_END      = "END";
	private final static String DEPLOYMENT_STATUS_PROGRESS = "PROGRESS";
	private final static String DEPLOYMENT_WARNING         = "WARNING";

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("deploy", DeployCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String mode = (String) attributes.get("mode");
		if (mode != null && "export".equals(mode)) {

			doExport(attributes);

		} else {

			// default is "import"
			doImport(attributes);
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

	public Gson getGson() {
		return new GsonBuilder().setPrettyPrinting().create();
	}

	// ----- public static methods -----
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


	// ----- private methods -----
	private void doImport(final Map<String, Object> attributes) throws FrameworkException {

		final long startTime = System.currentTimeMillis();
		customHeaders.put("start", new Date(startTime).toString());

		final String path                        = (String) attributes.get("source");

		final SecurityContext ctx = SecurityContext.getSuperUserInstance();
		final App app                            = StructrApp.getInstance(ctx);

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
		broadcastData.put("type", DEPLOYMENT_IMPORT_STATUS);
		broadcastData.put("subtype", DEPLOYMENT_STATUS_BEGIN);
		broadcastData.put("start", startTime);
		broadcastData.put("source", source);
		TransactionCommand.simpleBroadcastGenericMessage(broadcastData);

		// apply configuration
		final Path preDeployConf = source.resolve("pre-deploy.conf");
		if (Files.exists(preDeployConf)) {

			try (final Tx tx = app.tx()) {

				final String confSource = new String(Files.readAllBytes(preDeployConf), Charset.forName("utf-8")).trim();

				if (confSource.length() > 0) {

					info("Applying pre-deployment configuration from {}", preDeployConf);
					publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Applying pre-deployment configuration");

					Scripting.evaluate(new ActionContext(ctx), null, confSource, "pre-deploy.conf");
				} else {

					info("Ignoring empty pre-deployment configuration {}", preDeployConf);

				}

				tx.success();

			} catch (Throwable t) {
				logger.warn("", t);
				publishDeploymentWarningMessage("Exception caught while importing pre-deploy.conf", t.toString());
			}
		}

		// backup previous value of change log setting
		// temporary disable creation of change log
		final boolean changeLogEnabled = Settings.ChangelogEnabled.getValue();
		Settings.ChangelogEnabled.setValue(false);

		// read grants.json
		publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing resource access grants");

		final Path grantsConf = source.resolve("security/grants.json");
		if (Files.exists(grantsConf)) {

			info("Reading {}", grantsConf);
			importListData(ResourceAccess.class, readConfigList(grantsConf));
		}

		// read schema-methods.json
		final Path schemaMethodsConf = source.resolve("schema-methods.json");
		if (Files.exists(schemaMethodsConf)) {

			info("Reading {}", schemaMethodsConf);
			final String title = "Deprecation warning";
			final String text = "Found file 'schema-methods.json'. Newer versions store global schema methods in the schema snapshot file. Recreate the export with the current version to avoid compatibility issues. Support for importing this file will be dropped in future versions.";

			info(title + ": " + text);
			publishDeploymentWarningMessage(title, text);

			importListData(SchemaMethod.class, readConfigList(schemaMethodsConf));
		}

		// read mail-templates.json
		final Path mailTemplatesConf = source.resolve("mail-templates.json");
		if (Files.exists(mailTemplatesConf)) {

			info("Reading {}", mailTemplatesConf);
			publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing mail templates");

			importListData(MailTemplate.class, readConfigList(mailTemplatesConf));
		}

		// read widgets.json
		final Path widgetsConf = source.resolve("widgets.json");
		if (Files.exists(widgetsConf)) {

			info("Reading {}", widgetsConf);
			publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing widgets");

			importListData(Widget.class, readConfigList(widgetsConf));
		}

		// read localizations.json
		final Path localizationsConf = source.resolve("localizations.json");
		if (Files.exists(localizationsConf)) {

			final PropertyMap additionalData = new PropertyMap();

			// Question: shouldn't this be true? No, 'imported' is a flag for legacy-localization which
			// have been imported from a legacy-system which was replaced by structr.
			// it is a way to differentiate between new and old localization strings
			additionalData.put(Localization.imported, false);

			info("Reading {}", localizationsConf);
			publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing localizations");

			importListData(Localization.class, readConfigList(localizationsConf), additionalData);
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
		final Path schema = source.resolve("schema");
		if (Files.exists(schema)) {

			try {

				info("Importing data from schema/ directory");
				publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing schema");

				Files.walkFileTree(schema, new SchemaImportVisitor(schema));

			} catch (IOException ioex) {
				logger.warn("Exception while importing schema", ioex);
			}
		}

		// import files
		final Path files = source.resolve("files");
		if (Files.exists(files)) {

			try {

				info("Importing files (unchanged files will be skipped)");
				publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing files");

				FileImportVisitor fiv = new FileImportVisitor(files, filesConf);
				Files.walkFileTree(files, fiv);
				fiv.handleDeferredFiles();

			} catch (IOException ioex) {
				logger.warn("Exception while importing files", ioex);
			}
		}


		for (StructrModule module : StructrApp.getConfiguration().getModules().values()) {

			if (module.hasDeploymentData()) {

				info("Importing deployment data for module {}", module.getName());
				publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing deployment data for module " + module.getName());

				final Path moduleFolder = source.resolve("modules/" + module.getName() + "/");

				module.importDeploymentData(moduleFolder, getGson());
			}
		}


		// construct paths
		final Path templates  = source.resolve("templates");
		final Path components = source.resolve("components");
		final Path pages      = source.resolve("pages");

		// remove all DOMNodes from the database (clean webapp for import, but only
		// if the actual import directories exist, don't delete web components if
		// an empty directory was specified accidentially).
		if (Files.exists(templates) && Files.exists(components) && Files.exists(pages)) {

			try (final Tx tx = app.tx()) {

				final String tenantIdentifier = app.getDatabaseService().getTenantIdentifier();

				info("Removing pages, templates and components");
				publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Removing pages, templates and components");

				if (tenantIdentifier != null) {

					app.cypher("MATCH (n:" + tenantIdentifier + ":DOMNode) DETACH DELETE n", null);

				} else {

					app.cypher("MATCH (n:DOMNode) DETACH DELETE n", null);
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
				publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing templates");

				Files.walkFileTree(templates, new TemplateImportVisitor(templatesConf));

			} catch (IOException ioex) {
				logger.warn("Exception while importing templates", ioex);
			}
		}

		// import components, must be done before pages so the shared components exist
		if (Files.exists(components)) {

			try {

				info("Importing shared components");
				publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing shared components");

				Files.walkFileTree(components, new ComponentImportVisitor(componentsConf));

			} catch (IOException ioex) {
				logger.warn("Exception while importing shared components", ioex);
			}
		}

		// import pages
		if (Files.exists(pages)) {

			try {

				info("Importing pages");
				publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Importing pages");

				Files.walkFileTree(pages, new PageImportVisitor(pages, pagesConf));

			} catch (IOException ioex) {
				logger.warn("Exception while importing pages", ioex);
			}
		}

		try (final Tx tx = app.tx()) {

			deferredPageLinks.forEach((String linkableUUID, String pagePath) -> {

				try {
					final DOMNode page = StructrApp.getInstance().get(DOMNode.class, linkableUUID);

					final Linkable linkedPage = StructrApp.getInstance().nodeQuery(Linkable.class).and(Page.path, pagePath).or(Page.name, pagePath).getFirst();
					page.setProperties(page.getSecurityContext(), new PropertyMap(LinkSource.linkable, linkedPage));

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

				final String confSource = new String(Files.readAllBytes(postDeployConf), Charset.forName("utf-8")).trim();

				if (confSource.length() > 0) {

					info("Applying post-deployment configuration from {}", postDeployConf);
					publishDeploymentProgressMessage(DEPLOYMENT_IMPORT_STATUS, "Applying post-deployment configuration");

					Scripting.evaluate(new ActionContext(ctx), null, confSource, "post-deploy.conf");

				} else {

					info("Ignoring empty post-deployment configuration {}", postDeployConf);

				}

				tx.success();

			} catch (Throwable t) {
				logger.warn("", t);
				publishDeploymentWarningMessage("Exception caught while importing post-deploy.conf", t.toString());
			}
		}

		// restore saved value
		Settings.ChangelogEnabled.setValue(changeLogEnabled);

		final long endTime = System.currentTimeMillis();
		DecimalFormat decimalFormat  = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		final String duration = decimalFormat.format(((endTime - startTime) / 1000.0)) + "s";

		customHeaders.put("end", new Date(endTime).toString());
		customHeaders.put("duration", duration);

		info("Import from {} done. (Took {})", source.toString(), duration);

		broadcastData.put("subtype", DEPLOYMENT_STATUS_END);
		broadcastData.put("end", endTime);
		broadcastData.put("duration", duration);
		TransactionCommand.simpleBroadcastGenericMessage(broadcastData);

	}

	private void publishDeploymentProgressMessage (final String type, final String message) {

		final Map<String, Object> msgData = new HashMap();
		msgData.put("type", type);
		msgData.put("subtype", DEPLOYMENT_STATUS_PROGRESS);
		msgData.put("message", message);
		msgData.put("step", ++stepCounter);

		TransactionCommand.simpleBroadcastGenericMessage(msgData);

	}

	private void publishDeploymentWarningMessage (final String title, final String text) {

		final Map<String, Object> warningMsgData = new HashMap();
		warningMsgData.put("type", DEPLOYMENT_WARNING);
		warningMsgData.put("title", title);
		warningMsgData.put("text", text);

		TransactionCommand.simpleBroadcastGenericMessage(warningMsgData);

	}

	private void doExport(final Map<String, Object> attributes) throws FrameworkException {

		final String path  = (String) attributes.get("target");

		if (StringUtils.isBlank(path)) {

			throw new FrameworkException(422, "Please provide target path for deployment export.");
		}

		final Path target  = Paths.get(path);

		try {

			final long startTime = System.currentTimeMillis();
			customHeaders.put("start", new Date(startTime).toString());

			final Map<String, Object> broadcastData = new HashMap();
			broadcastData.put("type", DEPLOYMENT_EXPORT_STATUS);
			broadcastData.put("subtype", DEPLOYMENT_STATUS_BEGIN);
			broadcastData.put("start", startTime);
			broadcastData.put("target", target);
			TransactionCommand.simpleBroadcastGenericMessage(broadcastData);

			Files.createDirectories(target);

			final Path components     = Files.createDirectories(target.resolve("components"));
			final Path files          = Files.createDirectories(target.resolve("files"));
			final Path pages          = Files.createDirectories(target.resolve("pages"));
			final Path schema         = Files.createDirectories(target.resolve("schema"));
			final Path security       = Files.createDirectories(target.resolve("security"));
			final Path templates      = Files.createDirectories(target.resolve("templates"));
			final Path modules        = Files.createDirectories(target.resolve("modules"));
			final Path schemaJson     = schema.resolve("schema.json");
			final Path grants         = security.resolve("grants.json");
			final Path filesConf      = target.resolve("files.json");
			final Path pagesConf      = target.resolve("pages.json");
			final Path componentsConf = target.resolve("components.json");
			final Path templatesConf  = target.resolve("templates.json");
			final Path mailTemplates  = target.resolve("mail-templates.json");
			final Path localizations  = target.resolve("localizations.json");
			final Path widgets		  = target.resolve("widgets.json");

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Files");
			exportFiles(files, filesConf);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Pages");
			exportPages(pages, pagesConf);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Components");
			exportComponents(components, componentsConf);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Templates");
			exportTemplates(templates, templatesConf);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Resource Access Grants");
			exportResourceAccessGrants(grants);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Schema");
			exportSchema(schemaJson);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Mail Templates");
			exportMailTemplates(mailTemplates);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Localizations");
			exportLocalizations(localizations);

			publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting Widgets");
			exportWidgets(widgets);

			for (StructrModule module : StructrApp.getConfiguration().getModules().values()) {

				if (module.hasDeploymentData()) {
					logger.info("Exporting deployment data for module {}", module.getName());

					publishDeploymentProgressMessage(DEPLOYMENT_EXPORT_STATUS, "Exporting deployment data for module " + module.getName());

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

			broadcastData.put("subtype", DEPLOYMENT_STATUS_END);
			broadcastData.put("end", endTime);
			broadcastData.put("duration", duration);
			TransactionCommand.simpleBroadcastGenericMessage(broadcastData);

		} catch (IOException ex) {
			logger.warn("", ex);
		}
	}

	private void exportFiles(final Path target, final Path configTarget) throws FrameworkException {

		logger.info("Exporting files (unchanged files will be skipped)");

		final Map<String, Object> config = new TreeMap<>();
		final App app                    = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// fetch toplevel folders and recurse
			for (final Folder folder : app.nodeQuery(Folder.class).and(Folder.parent, null).sort(Folder.name).and(AbstractFile.includeInFrontendExport, true).getAsList()) {
				exportFilesAndFolders(target, folder, config);
			}

			// fetch toplevel files that are marked for export or for use as a javascript library
			for (final FileBase file : app.nodeQuery(FileBase.class)
				.and(Folder.parent, null)
				.sort(FileBase.name)
				.and()
					.or(AbstractFile.includeInFrontendExport, true)
					.or(FileBase.useAsJavascriptLibrary, true)
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

		// make sure that only frontend data is exported, ignore extended
		// types and those with relationships to user data.
		if (DeployCommand.okToExport(folder)) {

			final Map<String, Object> properties = new TreeMap<>();

			Files.createDirectories(path);

			exportFileConfiguration(folder, properties);

			if (!properties.isEmpty()) {
				config.put(folder.getPath(), properties);
			}
		}

		final List<Folder> folders = folder.getProperty(Folder.folders);
		Collections.sort(folders, new GraphObjectComparator(AbstractNode.name, false));

		for (final Folder child : folders) {
			exportFilesAndFolders(path, child, config);
		}

		final List<FileBase> files = folder.getProperty(Folder.files);
		Collections.sort(files, new GraphObjectComparator(AbstractNode.name, false));

		for (final FileBase file : files) {
			exportFile(path, file, config);
		}
	}

	private void exportFile(final Path target, final FileBase file, final Map<String, Object> config) throws IOException {

		if (!DeployCommand.okToExport(file)) {
			return;
		}

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
			config.put(file.getFolderPath(), properties);
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

				for (final DOMNode node : shadowDocument.getProperty(Page.elements)) {

					final boolean hasParent = node.getProperty(DOMNode.parent) != null;
					final boolean inTrash   = node.inTrash();
					boolean doExport        = true;

					// skip nodes in trash and non-toplevel nodes
					if (inTrash || hasParent) {
						continue;
					}

					final String content = node.getContent(RenderContext.EditMode.DEPLOYMENT);
					if (content != null) {

						String name = node.getProperty(AbstractNode.name);
						if (name == null) {

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

				final boolean inTrash     = template.inTrash();
				final boolean isShared    = template.getProperty(DOMNode.sharedComponent) != null;

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

		final String content = template.getProperty(Template.content);
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

				grant.put("signature", res.getProperty(ResourceAccess.signature));
				grant.put("flags",     res.getProperty(ResourceAccess.flags));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(grants, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void exportSchema(final Path target) throws FrameworkException {

		logger.info("Exporting schema");

		try {

			final JsonSchema schema = StructrSchema.createFromDatabase(StructrApp.getInstance());

			try (final Writer writer = new FileWriter(target.toFile())) {

				writer.append(schema.toString());
				writer.append("\n");
				writer.flush();

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}

		} catch (URISyntaxException x) {
			logger.warn("", x);
		}
	}

	private void exportConfiguration(final DOMNode node, final Map<String, Object> config) throws FrameworkException {

		if (node.isVisibleToPublicUsers())        { putIf(config, "visibleToPublicUsers", true); }
		if (node.isVisibleToAuthenticatedUsers()) { putIf(config, "visibleToAuthenticatedUsers", true); }

		putIf(config, "contentType",             node.getProperty(Content.contentType));

		if (node instanceof Template) {

			// mark this template as being shared
			putIf(config, "shared", Boolean.toString(node.isSharedComponent() && node.getProperty(DOMNode.parent) == null));
		}

		if (node instanceof Page) {
			putIf(config, "path",                    node.getProperty(Page.path));
			putIf(config, "position",                node.getProperty(Page.position));
			putIf(config, "category",                node.getProperty(Page.category));
			putIf(config, "showOnErrorCodes",        node.getProperty(Page.showOnErrorCodes));
			putIf(config, "showConditions",          node.getProperty(Page.showConditions));
			putIf(config, "hideConditions",          node.getProperty(Page.hideConditions));
			putIf(config, "dontCache",               node.getProperty(Page.dontCache));
			putIf(config, "cacheForSeconds",         node.getProperty(Page.cacheForSeconds));
			putIf(config, "pageCreatesRawData",      node.getProperty(Page.pageCreatesRawData));
			putIf(config, "basicAuthRealm",          node.getProperty(Page.basicAuthRealm));
			putIf(config, "enableBasicAuth",         node.getProperty(Page.enableBasicAuth));
		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(node.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys
			if (key.isDynamic()) {

				putIf(config, key.jsonName(), node.getProperty(key));
			}
		}
	}

	private void exportFileConfiguration(final AbstractFile file, final Map<String, Object> config) {

		if (file.isVisibleToPublicUsers())         { putIf(config, "visibleToPublicUsers", true); }
		if (file.isVisibleToAuthenticatedUsers())  { putIf(config, "visibleToAuthenticatedUsers", true); }
		if (file.getProperty(FileBase.isTemplate)) { putIf(config, "isTemplate", true); }

		putIf(config, "type",                        file.getProperty(FileBase.type));
		putIf(config, "contentType",                 file.getProperty(FileBase.contentType));
		putIf(config, "cacheForSeconds",             file.getProperty(FileBase.cacheForSeconds));
		putIf(config, "useAsJavascriptLibrary",      file.getProperty(FileBase.useAsJavascriptLibrary));
		putIf(config, "includeInFrontendExport",     file.getProperty(FileBase.includeInFrontendExport));
		putIf(config, "basicAuthRealm",              file.getProperty(FileBase.basicAuthRealm));
		putIf(config, "enableBasicAuth",             file.getProperty(FileBase.enableBasicAuth));

		if (file instanceof Image) {
			putIf(config, "isThumbnail",             file.getProperty(Image.isThumbnail));
			putIf(config, "isImage",                 file.getProperty(Image.isImage));
			putIf(config, "width",                   file.getProperty(Image.width));
			putIf(config, "height",                  file.getProperty(Image.height));
		}

		if (file instanceof AbstractMinifiedFile) {

			if (file instanceof MinifiedCssFile) {
				putIf(config, "lineBreak",               file.getProperty(MinifiedCssFile.lineBreak));
			}

			if (file instanceof MinifiedJavaScriptFile) {
				putIf(config, "optimizationLevel",       file.getProperty(MinifiedJavaScriptFile.optimizationLevel));
				putIf(config, "minificationSources",     file.getProperty(MinifiedJavaScriptFile.minificationSources));
			}

			Map<Integer, String> minifcationSources = new TreeMap<>();
			for(MinificationSource minificationSourceRel : file.getOutgoingRelationships(MinificationSource.class)) {
				minifcationSources.put(minificationSourceRel.getProperty(MinificationSource.position), minificationSourceRel.getTargetNode().getFolderPath());
			}
			putIf(config, "minificationSources",     minifcationSources);

		}

		// export all dynamic properties
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(file.getClass(), PropertyView.All)) {

			// only export dynamic (=> additional) keys
			if (key.isDynamic()) {

				putIf(config, key.jsonName(), file.getProperty(key));
			}
		}

		exportOwnershipAndSecurity(file, config);
	}

	private void exportOwnershipAndSecurity(final AbstractNode node, final Map<String, Object> config) {

		// export unique name of owner node to pages.json
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

	private void exportMailTemplates(final Path target) throws FrameworkException {

		logger.info("Exporting mail templates");

		final List<Map<String, Object>> mailTemplates = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final MailTemplate mailTemplate : app.nodeQuery(MailTemplate.class).sort(MailTemplate.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				mailTemplates.add(entry);

				entry.put("name",   mailTemplate.getProperty(MailTemplate.name));
				entry.put("text",   mailTemplate.getProperty(MailTemplate.text));
				entry.put("locale", mailTemplate.getProperty(MailTemplate.locale));
				entry.put("visibleToAuthenticatedUsers", mailTemplate.getProperty(MailTemplate.visibleToAuthenticatedUsers));
				entry.put("visibleToPublicUsers", mailTemplate.getProperty(MailTemplate.visibleToPublicUsers));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

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

				entry.put("name",   widget.getProperty(Widget.name));
				entry.put("source",   widget.getProperty(Widget.source));
				entry.put("description",   widget.getProperty(Widget.description));
				entry.put("isWidget",   widget.getProperty(Widget.isWidget));
				entry.put("treePath",   widget.getProperty(Widget.treePath));
				entry.put("pictures",   widget.getProperty(Widget.pictures));
				entry.put("configuration", widget.getProperty(Widget.configuration));
				entry.put("visibleToAuthenticatedUsers", widget.getProperty(Widget.visibleToAuthenticatedUsers));
				entry.put("visibleToPublicUsers", widget.getProperty(Widget.visibleToPublicUsers));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(widgets, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void exportLocalizations(final Path target) throws FrameworkException {

		logger.info("Exporting localizations");

		final List<Map<String, Object>> localizations = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final Localization localization : app.nodeQuery(Localization.class).sort(Localization.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				localizations.add(entry);

				entry.put("name",                        localization.getProperty(Localization.name));
				entry.put("localizedName",               localization.getProperty(Localization.localizedName));
				entry.put("domain",                      localization.getProperty(Localization.domain));
				entry.put("locale",                      localization.getProperty(Localization.locale));
				entry.put("imported",                    localization.getProperty(Localization.imported));
				entry.put("visibleToAuthenticatedUsers", localization.getProperty(MailTemplate.visibleToAuthenticatedUsers));
				entry.put("visibleToPublicUsers",        localization.getProperty(MailTemplate.visibleToPublicUsers));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(localizations, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private void putIf(final Map<String, Object> target, final String key, final Object value) {

		if (value != null) {
			target.put(key, value);
		}
	}

	private List<Map<String, Object>> readConfigList(final Path conf) {

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

			for (final T toDelete : app.nodeQuery(type).getAsList()) {
				app.delete(toDelete);
			}

			for (final Map<String, Object> entry : data) {

				final PropertyMap map = PropertyMap.inputTypeToJavaType(context, type, entry);

				// allow caller to insert additional data for better creation performance
				for (final PropertyMap add : additionalData) {
					map.putAll(add);
				}

				app.create(type, map);
			}

			tx.success();
		}
	}

	// ----- public static methods -----
	public static boolean okToExport(final AbstractFile file) {

		for (final AbstractRelationship rel : file.getRelationships()) {

			if (rel instanceof Security) {
				continue;
			}

			if (rel instanceof PrincipalOwnsNode) {
				continue;
			}

			if (rel instanceof FolderChildren) {
				continue;
			}

			if (rel instanceof FileChildren) {
				continue;
			}

			if (rel instanceof FileSiblings) {
				continue;
			}

			if (rel instanceof MinificationSource) {
				continue;
			}

			if (rel instanceof UserFavoriteFile) {
				continue;
			}

			if (rel instanceof UserWorkDir) {
				continue;
			}

			if (rel instanceof Folders) {
				continue;
			}

			if (rel instanceof org.structr.web.entity.relation.Files) {
				continue;
			}

			if (rel instanceof Images) {
				continue;
			}

			if (rel instanceof Thumbnails) {
				continue;
			}

			if (rel instanceof ResourceLink) {
				continue;
			}

			if (rel instanceof UserFavoriteFavoritable) {
				continue;
			}

			// if none of the above matched, the file should not be exported
			return false;
		}

		return true;
	}

	public static void addDeferredPagelink (String linkableUUID, String pagePath) {
		deferredPageLinks.put(linkableUUID, pagePath);
	}
}
