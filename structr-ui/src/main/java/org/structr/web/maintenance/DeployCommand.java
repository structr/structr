/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.GraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Group;
import org.structr.core.entity.Localization;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.Security;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonSchema;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.User;
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
import org.structr.web.entity.relation.UserFavoriteFile;
import org.structr.web.maintenance.deploy.ComponentImportVisitor;
import org.structr.web.maintenance.deploy.FileImportVisitor;
import org.structr.web.maintenance.deploy.PageImportVisitor;
import org.structr.web.maintenance.deploy.SchemaImportVisitor;
import org.structr.web.maintenance.deploy.TemplateImportVisitor;

/**
 *
 */
public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger                   = LoggerFactory.getLogger(BulkMoveUnusedFilesCommand.class.getName());
	private static final Pattern pattern                 = Pattern.compile("[a-f0-9]{32}");
	private static final Set<String> exportFileTypes     = new HashSet<>(Arrays.asList(new String[] { "File", "Folder", "Image" } ));

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

	public Map<String, Object> readConfigMap(final Path pagesConf) {

		if (Files.exists(pagesConf)) {

			try (final Reader reader = Files.newBufferedReader(pagesConf, Charset.forName("utf-8"))) {

				return new HashMap<>(getGson().fromJson(reader, Map.class));

			} catch (IOException ioex) {
				ioex.printStackTrace();
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


	// ----- private methods -----
	private void doImport(final Map<String, Object> attributes) throws FrameworkException {

		final String path                        = (String) attributes.get("source");
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

		// read users.json
		final Path usersConf = source.resolve("security/users.json");
		if (Files.exists(usersConf)) {

			info("Reading {}..", usersConf);
			importListData(User.class, readConfigList(usersConf));
		}

		// read users.json
		final Path groupsConf = source.resolve("security/groups.json");
		if (Files.exists(groupsConf)) {

			info("Reading {}..", groupsConf);
			importListData(Group.class, readConfigList(groupsConf));
		}

		// read grants.json
		final Path grantsConf = source.resolve("security/grants.json");
		if (Files.exists(grantsConf)) {

			info("Reading {}..", grantsConf);
			importListData(ResourceAccess.class, readConfigList(grantsConf));
		}

		// read mail-templates.json
		final Path mailTemplatesConf = source.resolve("mail-templates.json");
		if (Files.exists(mailTemplatesConf)) {

			info("Reading {}..", mailTemplatesConf);
			importListData(MailTemplate.class, readConfigList(mailTemplatesConf));
		}

		// read localizations.json
		final Path localizationsConf = source.resolve("localizations.json");
		if (Files.exists(localizationsConf)) {

			info("Reading {}..", localizationsConf);
			importListData(Localization.class, readConfigList(localizationsConf));
		}

		// read files.conf
		final Path filesConfFile = source.resolve("files.json");
		if (Files.exists(filesConfFile)) {

			info("Reading {}..", filesConfFile);
			filesConf.putAll(readConfigMap(filesConfFile));
		}

		// read pages.conf
		final Path pagesConfFile = source.resolve("pages.json");
		if (Files.exists(pagesConfFile)) {

			info("Reading {}..", pagesConfFile);
			pagesConf.putAll(readConfigMap(pagesConfFile));
		}

		// read components.conf
		final Path componentsConfFile = source.resolve("components.json");
		if (Files.exists(componentsConfFile)) {

			info("Reading {}..", componentsConfFile);
			componentsConf.putAll(readConfigMap(componentsConfFile));
		}

		// read templates.conf
		final Path templatesConfFile = source.resolve("templates.json");
		if (Files.exists(templatesConfFile)) {

			info("Reading {}..", templatesConfFile);
			templatesConf.putAll(readConfigMap(templatesConfFile));
		}

		// import schema
		final Path schema = source.resolve("schema");
		if (Files.exists(schema)) {

			try {

				info("Importing data from schema/ directory..");
				Files.walkFileTree(schema, new SchemaImportVisitor(schema));

			} catch (IOException ioex) {
				logger.warn("Exception while importing schema", ioex);
			}
		}

		// import files
		final Path files = source.resolve("files");
		if (Files.exists(files)) {

			try {

				info("Importing files...");
				Files.walkFileTree(files, new FileImportVisitor(files, filesConf));

			} catch (IOException ioex) {
				logger.warn("Exception while importing files", ioex);
			}
		}

		// import templates, must be done before pages so the templates exist
		final Path templates = source.resolve("templates");
		if (Files.exists(templates)) {

			try {

				info("Importing templates..");
				Files.walkFileTree(templates, new TemplateImportVisitor(templatesConf));

			} catch (IOException ioex) {
				logger.warn("Exception while importing templates", ioex);
			}
		}

		// import components, must be done before pages so the shared components exist
		final Path components = source.resolve("components");
		if (Files.exists(components)) {

			try {

				info("Importing shared components..");
				Files.walkFileTree(components, new ComponentImportVisitor(componentsConf));

			} catch (IOException ioex) {
				logger.warn("Exception while importing shared components", ioex);
			}
		}

		// import pages
		final Path pages = source.resolve("pages");
		if (Files.exists(pages)) {

			try {

				info("Importing pages..");
				Files.walkFileTree(pages, new PageImportVisitor(pages, pagesConf));

			} catch (IOException ioex) {
				logger.warn("Exception while importing pages", ioex);
			}
		}

		// apply configuration
		final Path conf = source.resolve("deploy.conf");
		if (Files.exists(conf)) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				info("Applying configuration from {}..", conf);

				final String confSource = new String(Files.readAllBytes(conf), Charset.forName("utf-8"));
				Scripting.evaluate(new ActionContext(SecurityContext.getSuperUserInstance()), null, confSource.trim(), "deploy.conf");

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		info("Import from {} done.", source.toString());
	}

	private void doExport(final Map<String, Object> attributes) throws FrameworkException {

		final String path  = (String) attributes.get("target");

		if (StringUtils.isBlank(path)) {

			throw new FrameworkException(422, "Please provide target path for deployment export.");
		}

		final Path target  = Paths.get(path);

		try {

			Files.createDirectories(target);

			final Path components     = Files.createDirectories(target.resolve("components"));
			final Path files          = Files.createDirectories(target.resolve("files"));
			final Path pages          = Files.createDirectories(target.resolve("pages"));
			final Path schema         = Files.createDirectories(target.resolve("schema"));
			final Path security       = Files.createDirectories(target.resolve("security"));
			final Path templates      = Files.createDirectories(target.resolve("templates"));
			final Path schemaJson     = schema.resolve("schema.json");
			final Path grants         = security.resolve("grants.json");
			final Path users          = security.resolve("users.json");
			final Path groups         = security.resolve("groups.json");
			final Path filesConf      = target.resolve("files.json");
			final Path pagesConf      = target.resolve("pages.json");
			final Path componentsConf = target.resolve("components.json");
			final Path templatesConf  = target.resolve("templates.json");
			final Path mailTemplates  = target.resolve("mail-templates.json");
			final Path localizations  = target.resolve("localizations.json");

			exportFiles(files, filesConf);
			exportPages(pages, pagesConf);
			exportComponents(components, componentsConf);
			exportTemplates(templates, templatesConf);
			exportResourceAccessGrants(grants);
			exportGroups(groups);
			exportUsers(users);
			exportSchema(schemaJson);
			exportMailTemplates(mailTemplates);
			exportLocalizations(localizations);

			// config import order is "users, grants, pages, components, templates"
			// data import order is "schema, files, templates, components, pages"

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void exportFiles(final Path target, final Path configTarget) throws FrameworkException {

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
			ioex.printStackTrace();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(configTarget.toFile()))) {

			getGson().toJson(config, fos);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	private void exportFilesAndFolders(final Path target, final Folder folder, final Map<String, Object> config) throws IOException {

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

		// modify file name if there are duplicates in the database
		if (Files.exists(targetPath)) {

			// compare checksum
			final Long checksumOfExistingFile = FileHelper.getChecksum(targetPath.toFile());
			final Long checksumOfExportFile   = file.getChecksum();

			if (checksumOfExistingFile.equals(checksumOfExportFile)) {

				logger.info("Skipping export of file {}, no changes.", name);
				doExport = false;
			}
		}

		// export only if file is
		if (doExport) {

			try {
				Files.copy(src, targetPath);

			} catch (IOException ioex) {
				logger.warn("Unable to write file {}: {}", targetPath.toString(), ioex.getMessage());
			}
		}

		exportFileConfiguration(file, properties);

		if (!properties.isEmpty()) {
			config.put(file.getPath(), properties);
		}
	}

	private void exportPages(final Path target, final Path configTarget) throws FrameworkException {

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
								if (existingContent.equals(content)) {

									logger.info("Skipping export of page {}, no changes.", name);
									doExport = false;
								}

							} catch (IOException ignore) {}
						}

						pagesConfig.put(name, properties);
						exportConfiguration(page, properties);

						if (doExport) {

							try (final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(pageFile.toFile()))) {

								writer.write(content);
								writer.flush();
								writer.close();

							} catch (IOException ioex) {
								ioex.printStackTrace();
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
			ioex.printStackTrace();
		}
	}

	private void exportComponents(final Path target, final Path configTarget) throws FrameworkException {

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
								if (existingContent.equals(content)) {

									logger.info("Skipping export of component {}, no changes.", name);
									doExport = false;
								}

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
								ioex.printStackTrace();
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
			ioex.printStackTrace();
		}
	}

	private void exportTemplates(final Path target, final Path configTarget) throws FrameworkException {

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
			ioex.printStackTrace();
		}
	}

	private void exportTemplateSource(final Path target, final DOMNode template, final Map<String, Object> configuration) throws FrameworkException {

		final Map<String, Object> properties = new TreeMap<>();
		boolean doExport                     = true;

		final String content = template.getProperty(Template.content);
		if (content != null) {

			// name or uuid
			String name = template.getProperty(AbstractNode.name);
			if (name == null) {

				name = template.getUuid();
			}

			final Path targetFile = target.resolve(name + ".html");

			if (Files.exists(targetFile)) {

				try {
					final String existingContent = new String(Files.readAllBytes(targetFile), "utf-8");
					if (existingContent.equals(content)) {

						logger.info("Skipping export of template {}, no changes.", name);
						doExport = false;
					}

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
					ioex.printStackTrace();
				}
			}
		}
	}

	private void exportResourceAccessGrants(final Path target) throws FrameworkException {

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
			ioex.printStackTrace();
		}
	}

	private void exportGroups(final Path target) throws FrameworkException {

		final List<Map<String, Object>> groups = new LinkedList<>();
		final App app                          = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final Group group : app.nodeQuery(Group.class).sort(Group.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				final List<String> members      = new LinkedList<>();
				groups.add(entry);

				entry.put("name",    group.getProperty(Group.name));
				entry.put("members", members);

				for (final Principal member : group.getProperty(Group.members)) {
					members.add(member.getProperty(Principal.name));
				}
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(groups, fos);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	private void exportUsers(final Path target) throws FrameworkException {

		final List<Map<String, Object>> users = new LinkedList<>();
		final App app                          = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final User user : app.nodeQuery(User.class).sort(User.name).getAsList()) {

				final Map<String, Object> grant = new TreeMap<>();
				users.add(grant);

				grant.put("name",         user.getProperty(User.name));
				grant.put("eMail",        user.getProperty(Principal.eMail));
				grant.put("isAdmin",      user.getProperty(Principal.isAdmin));
				grant.put("frontendUser", user.getProperty(User.frontendUser));
				grant.put("backendUser",  user.getProperty(User.backendUser));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(users, fos);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	private void exportSchema(final Path target) throws FrameworkException {

		try {

			final JsonSchema schema = StructrSchema.createFromDatabase(StructrApp.getInstance());

			try (final Writer writer = new FileWriter(target.toFile())) {

				writer.append(schema.toString());
				writer.append("\n");
				writer.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

		} catch (URISyntaxException x) {
			x.printStackTrace();
		}
	}

	private void exportConfiguration(final DOMNode node, final Map<String, Object> config) throws FrameworkException {

		if (node.isVisibleToPublicUsers())        { putIf(config, "visibleToPublicUsers", true); }
		if (node.isVisibleToAuthenticatedUsers()) { putIf(config, "visibleToAuthenticatedUsers", true); }

		putIf(config, "contentType",             node.getProperty(Content.contentType));

		if (node instanceof Template) {

			// mark this template as being shared
			putIf(config, "shared", Boolean.toString(node.isSharedComponent()));
		}

		if (node instanceof Page) {
			putIf(config, "position",                node.getProperty(Page.position));
			putIf(config, "showOnErrorCodes",        node.getProperty(Page.showOnErrorCodes));
			putIf(config, "showConditions",          node.getProperty(Page.showConditions));
			putIf(config, "hideConditions",          node.getProperty(Page.hideConditions));
			putIf(config, "dontCache",               node.getProperty(Page.dontCache));
			putIf(config, "cacheForSeconds",         node.getProperty(Page.cacheForSeconds));
			putIf(config, "pageCreatesRawData",      node.getProperty(Page.pageCreatesRawData));
			putIf(config, "basicAuthRealm",          node.getProperty(Page.basicAuthRealm));
			putIf(config, "enableBasicAuth",         node.getProperty(Page.enableBasicAuth));
		}

		exportOwnershipAndSecurity(node, config);
	}

	private void exportFileConfiguration(final AbstractFile file, final Map<String, Object> config) {

		if (file.isVisibleToPublicUsers())        { putIf(config, "visibleToPublicUsers", true); }
		if (file.isVisibleToAuthenticatedUsers()) { putIf(config, "visibleToAuthenticatedUsers", true); }

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
				grant.put("allowed", StringUtils.join(security.getPermissions(), ","));

				grantees.add(grant);
			}
		}

		// export non-empty collection only
		if (!grantees.isEmpty()) {
			config.put("grantees", grantees);
		}
	}

	private void exportMailTemplates(final Path target) throws FrameworkException {

		final List<Map<String, Object>> mailTemplates = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final MailTemplate mailTemplate : app.nodeQuery(MailTemplate.class).sort(MailTemplate.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				mailTemplates.add(entry);

				entry.put("name",   mailTemplate.getProperty(MailTemplate.name));
				entry.put("text",   mailTemplate.getProperty(MailTemplate.text));
				entry.put("locale", mailTemplate.getProperty(MailTemplate.locale));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(mailTemplates, fos);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	private void exportLocalizations(final Path target) throws FrameworkException {

		final List<Map<String, Object>> localizations = new LinkedList<>();
		final App app                                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final Localization localization : app.nodeQuery(Localization.class).sort(Localization.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();
				localizations.add(entry);

				entry.put("name",          localization.getProperty(Localization.name));
				entry.put("localizedName", localization.getProperty(Localization.localizedName));
				entry.put("domain",        localization.getProperty(Localization.domain));
				entry.put("locale",        localization.getProperty(Localization.locale));
				entry.put("imported",      localization.getProperty(Localization.imported));
			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(target.toFile()))) {

			getGson().toJson(localizations, fos);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}

	private void putIf(final Map<String, Object> target, final String key, final Object value) {

		if (value != null) {
			target.put(key, value);
		}
	}

	private List<Map<String, Object>> readConfigList(final Path pagesConf) {

		try (final Reader reader = Files.newBufferedReader(pagesConf, Charset.forName("utf-8"))) {

			return getGson().fromJson(reader, List.class);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		return Collections.emptyList();
	}

	private <T extends NodeInterface> void importMapData(final Class<T> type, final Map<String, Object> data) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		final App app                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final T toDelete : app.nodeQuery(type).getAsList()) {
				app.delete(toDelete);
			}

			for (final Entry<String, Object> entry : data.entrySet()) {

				final String key = entry.getKey();
				final Object val = entry.getValue();

				if (val instanceof Map) {

					final Map<String, Object> values = (Map<String, Object>)val;
					final PropertyMap properties     = PropertyMap.inputTypeToJavaType(context, type, values);

					properties.put(AbstractNode.name, key);

					app.create(type, properties);
				}
			}

			tx.success();
		}
	}

	private <T extends NodeInterface> void importListData(final Class<T> type, final List<Map<String, Object>> data) throws FrameworkException {

		final SecurityContext context = SecurityContext.getSuperUserInstance();
		final App app                 = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final T toDelete : app.nodeQuery(type).getAsList()) {
				app.delete(toDelete);
			}

			for (final Map<String, Object> entry : data) {
				app.create(type, PropertyMap.inputTypeToJavaType(context, type, entry));
			}

			tx.success();
		}
	}

	// ----- public static methods -----
	public static boolean okToExport(final AbstractFile file) {

		// export non-derived types only (ignore things that extend built-in file/folder etc.)
		if (!exportFileTypes.contains(file.getType())) {
			return false;
		}

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

			// if none of the above matched, the file should not be exported
			return false;
		}

		return true;
	}
}
