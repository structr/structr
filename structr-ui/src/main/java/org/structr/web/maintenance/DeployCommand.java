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
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;
import org.structr.web.maintenance.deploy.ComponentImportVisitor;
import org.structr.web.maintenance.deploy.FileImportVisitor;
import org.structr.web.maintenance.deploy.PageImportVisitor;
import org.structr.web.maintenance.deploy.SchemaImportVisitor;
import org.structr.web.maintenance.deploy.TemplateImportVisitor;

/**
 *
 */
public class DeployCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(BulkMoveUnusedFilesCommand.class.getName());

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("deploy", DeployCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String path                        = (String) attributes.get("source");
		final Map<String, Object> componentsConf = new HashMap<>();
		final Map<String, Object> templatesConf  = new HashMap<>();
		final Map<String, Object> pagesConf      = new HashMap<>();

		if (StringUtils.isBlank(path)) {

			throw new FrameworkException(422, "Please provide source path for deployment.");
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

			logger.log(Level.INFO, "Reading {0}..", usersConf);
			importMapData(User.class, readConfigMap(usersConf));
		}

		// read grants.json
		final Path grantsConf = source.resolve("security/grants.json");
		if (Files.exists(grantsConf)) {

			logger.log(Level.INFO, "Reading {0}..", grantsConf);
			importListData(ResourceAccess.class, readConfigList(grantsConf));
		}

		// read pages.conf
		final Path pagesConfFile = source.resolve("pages.json");
		if (Files.exists(pagesConfFile)) {

			logger.log(Level.INFO, "Reading {0}..", pagesConfFile);
			pagesConf.putAll(readConfigMap(pagesConfFile));
		}

		// read components.conf
		final Path componentsConfFile = source.resolve("components.json");
		if (Files.exists(componentsConfFile)) {

			logger.log(Level.INFO, "Reading {0}..", componentsConfFile);
			componentsConf.putAll(readConfigMap(componentsConfFile));
		}

		// read templates.conf
		final Path templatesConfFile = source.resolve("templates.json");
		if (Files.exists(templatesConfFile)) {

			logger.log(Level.INFO, "Reading {0}..", templatesConfFile);
			templatesConf.putAll(readConfigMap(templatesConfFile));
		}

		// import schema
		final Path schema = source.resolve("schema");
		if (Files.exists(schema)) {

			try {

				logger.log(Level.INFO, "Importing data from schema/ directory..");
				Files.walkFileTree(schema, new SchemaImportVisitor(schema));

			} catch (IOException ioex) {
				logger.log(Level.WARNING, "Exception while importing schema", ioex);
			}
		}

		// import components, must be done before pages so the shared components exist
		final Path templates = source.resolve("templates");
		if (Files.exists(templates)) {

			try {

				logger.log(Level.INFO, "Importing templates..");
				Files.walkFileTree(templates, new TemplateImportVisitor(templatesConf));

			} catch (IOException ioex) {
				logger.log(Level.WARNING, "Exception while importing templates", ioex);
			}
		}

		// import components, must be done before pages so the shared components exist
		final Path components = source.resolve("components");
		if (Files.exists(components)) {

			try {

				logger.log(Level.INFO, "Importing shared components..");
				Files.walkFileTree(components, new ComponentImportVisitor(componentsConf));

			} catch (IOException ioex) {
				logger.log(Level.WARNING, "Exception while importing shared components", ioex);
			}
		}

		// import files
		final Path files = source.resolve("files");
		if (Files.exists(files)) {

			try {

				logger.log(Level.INFO, "Importing files...");
				Files.walkFileTree(files, new FileImportVisitor(files));

			} catch (IOException ioex) {
				logger.log(Level.WARNING, "Exception while importing files", ioex);
			}
		}

		// import pages
		final Path pages = source.resolve("pages");
		if (Files.exists(pages)) {

			try {

				logger.log(Level.INFO, "Importing pages..");
				Files.walkFileTree(pages, new PageImportVisitor(pages, pagesConf));

			} catch (IOException ioex) {
				logger.log(Level.WARNING, "Exception while importing pages", ioex);
			}
		}

		// apply configuration
		final Path conf = source.resolve("deploy.conf");
		if (Files.exists(conf)) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				logger.log(Level.INFO, "Applying configuration from {0}..", conf);

				final String confSource = new String(Files.readAllBytes(conf), Charset.forName("utf-8"));
				Scripting.evaluate(new ActionContext(SecurityContext.getSuperUserInstance()), null, confSource.trim());

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		logger.log(Level.INFO, "Import from {0} done.", source.toString());
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	// ----- private methods -----
	private Map<String, Object> readConfigMap(final Path pagesConf) {

		final Gson gson = new GsonBuilder().create();

		try (final Reader reader = Files.newBufferedReader(pagesConf, Charset.forName("utf-8"))) {

			return gson.fromJson(reader, Map.class);

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		return Collections.emptyMap();
	}

	private List<Map<String, Object>> readConfigList(final Path pagesConf) {

		final Gson gson = new GsonBuilder().create();

		try (final Reader reader = Files.newBufferedReader(pagesConf, Charset.forName("utf-8"))) {

			return gson.fromJson(reader, List.class);

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
}
