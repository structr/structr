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
package org.structr.rest.resource;


import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Task;
import org.structr.api.schema.InvalidSchemaException;
import org.structr.api.schema.JsonSchema;
import org.structr.api.search.SortOrder;
import org.structr.api.service.Command;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.*;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.*;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.importer.SchemaAnalyzer;
import org.structr.schema.importer.SchemaJsonImporter;
import org.structr.util.StructrLicenseManager;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * A resource constraint that allows to execute maintenance tasks via
 * REST API.
 *
 *
 */
public class MaintenanceResource extends ExactMatchEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceResource.class);

	private static final Map<String, Class> maintenanceCommandMap = new LinkedHashMap<>();

	static {

		maintenanceCommandMap.put("rebuildIndex",               BulkRebuildIndexCommand.class);
		maintenanceCommandMap.put("rebuildIndexForType",        BulkRebuildIndexCommand.class);
		maintenanceCommandMap.put("createLabels",               BulkCreateLabelsCommand.class);
		maintenanceCommandMap.put("clearDatabase",              ClearDatabase.class);
		maintenanceCommandMap.put("fixNodeProperties",          BulkFixNodePropertiesCommand.class);
		maintenanceCommandMap.put("setNodeProperties",          BulkSetNodePropertiesCommand.class);
		maintenanceCommandMap.put("changeNodePropertyKey",      BulkChangeNodePropertyKeyCommand.class);
		maintenanceCommandMap.put("setRelationshipProperties",  BulkSetRelationshipPropertiesCommand.class);
		maintenanceCommandMap.put("copyRelationshipProperties", BulkCopyRelationshipPropertyCommand.class);
		maintenanceCommandMap.put("createLicense",              StructrLicenseManager.CreateLicenseCommand.class);
		maintenanceCommandMap.put("updateLicense",              StructrLicenseManager.UpdateLicenseCommand.class);
		maintenanceCommandMap.put("setUuid",                    BulkSetUuidCommand.class);
		maintenanceCommandMap.put("sync",                       SyncCommand.class);
		maintenanceCommandMap.put("flushCaches",                FlushCachesCommand.class);
		maintenanceCommandMap.put("analyzeSchema",              SchemaAnalyzer.class);
		maintenanceCommandMap.put("migrateChangelog",           BulkMigrateChangelogCommand.class);
		maintenanceCommandMap.put("manageDatabases",            ManageDatabasesCommand.class);
		maintenanceCommandMap.put("manageThreads",              ManageThreadsCommand.class);
	}

	public MaintenanceResource() {

		super(
			RESTParameter.forStaticString("maintenance", true),
			RESTParameter.forPattern("name", "[a-zA-Z_]+", true)
		);
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		final String typeName = call.get("name");
		if (typeName != null) {

			if ("_schemaJson".equals(typeName)) {

				// handle schema json
				return new SchemaJsonResourceHandler(call);
			}

			if (maintenanceCommandMap.containsKey(typeName)) {

				// handle maintenance command
				return new MaintenanceResourceHandler(call, typeName, maintenanceCommandMap.get(typeName));
			}

		}

		return null;
	}

	public static void registerMaintenanceTask(final String key, final Class<? extends Task> task) {

		if(maintenanceCommandMap.containsKey(key)) {
			throw new IllegalStateException("Maintenance command for key " + key + " already registered!");
		}

		maintenanceCommandMap.put(key, task);
	}

	public static void registerMaintenanceCommand(final String key, final Class<? extends Command> command) {

		if(maintenanceCommandMap.containsKey(key)) {
			throw new IllegalStateException("Maintenance command for key " + key + " already registered!");
		}

		maintenanceCommandMap.put(key, command);
	}

	public static Class getMaintenanceCommandClass(final String key) {
		return maintenanceCommandMap.get(key);
	}

	private class MaintenanceResourceHandler extends RESTCallHandler {

		private String taskOrCommandName = null;
		private Class taskOrCommand      = null;

		public MaintenanceResourceHandler(final RESTCall call, final String taskOrCommandName, final Class taskOrCommand) {

			super(call);

			this.taskOrCommandName = taskOrCommandName;
			this.taskOrCommand     = taskOrCommand;
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			throw new IllegalMethodException("GET not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
		}

		@Override
		public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("PUT not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
		}

		@Override
		public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {
			throw new IllegalMethodException("DELETE not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			if (securityContext != null && isSuperUser(securityContext)) {

				if (this.taskOrCommand != null) {

					RuntimeEventLog.maintenance(taskOrCommand.getSimpleName(), propertySet);

					try {

						final App app = StructrApp.getInstance(securityContext);

						if (Task.class.isAssignableFrom(taskOrCommand)) {

							final Task task = (Task) taskOrCommand.getDeclaredConstructor().newInstance();

							app.processTasks(task);

						} else if (MaintenanceCommand.class.isAssignableFrom(taskOrCommand)) {

							final MaintenanceCommand cmd = (MaintenanceCommand)StructrApp.getInstance(securityContext).command(taskOrCommand);

							// flush caches if required
							if (cmd.requiresFlushingOfCaches()) {

								logger.info("Flushing caches before maintenance command.");

								app.command(FlushCachesCommand.class).execute(Collections.EMPTY_MAP);
							}

							// create enclosing transaction if required
							if (cmd.requiresEnclosingTransaction()) {

								try (final Tx tx = app.tx()) {

									cmd.execute(propertySet);
									tx.success();
								}

							} else {

								cmd.execute(propertySet);
							}

							final RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_OK);

							result.setNonGraphObjectResult(cmd.getCommandResult());
							cmd.getCustomHeaders().forEach(result::addHeader);
							cmd.getCustomHeaders().clear();

							return result;

						} else {
							return new RestMethodResult(HttpServletResponse.SC_NOT_FOUND);
						}

						// return 200 OK
						return new RestMethodResult(HttpServletResponse.SC_OK);

					} catch (NoSuchMethodException|InvocationTargetException|InstantiationException|IllegalAccessException ex) {
						throw new SystemException(ex.getMessage());
					}

				} else {

					if (taskOrCommandName != null) {

						throw new NotFoundException("No such task or command: " + this.taskOrCommandName);

					} else {

						throw new IllegalPathException("Maintenance resource needs parameter");
					}
				}

			} else {

				throw new NotAllowedException("Use of the maintenance endpoint is restricted to admin users");

			}
		}

		@Override
		public boolean createPostTransaction() {
			return false;
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {
			return null;
		}


		@Override
		public boolean isCollection() {
			return true;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("POST");
		}

		// ----- private methods -----
		private boolean isSuperUser(final SecurityContext securityContext) throws FrameworkException {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final boolean isSuperUser = securityContext.isSuperUser();

				tx.success();

				return isSuperUser;
			}
		}
	}

	public class SchemaJsonResourceHandler extends RESTCallHandler {

		public SchemaJsonResourceHandler(final RESTCall call) {
			super(call);
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {
			return null;
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			final JsonSchema jsonSchema = StructrSchema.createFromDatabase(StructrApp.getInstance());
			final String schema         = jsonSchema.toString();

			return new PagingIterable<>(getURL(), Arrays.asList(schema));

		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			if(propertySet != null && propertySet.containsKey("schema")) {

				try {
					final App app           = StructrApp.getInstance(securityContext);
					final String schemaJson = (String)propertySet.get("schema");

					StructrSchema.replaceDatabaseSchema(app, StructrSchema.createFromSource(schemaJson));

					return new RestMethodResult(200, "Schema imported successfully");

				} catch (InvalidSchemaException | URISyntaxException ex) {

					return new RestMethodResult(422, ex.getMessage());
				}
			}

			return new RestMethodResult(400, "Invalid request body. Specify schema json string as 'schema' in request body.");
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("GET", "OPTIONS", "POST");
		}
	}
}
