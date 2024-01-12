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
package org.structr.rest.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.structr.agent.Task;
import org.structr.api.search.SortOrder;
import org.structr.api.service.Command;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.*;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.maintenance.SnapshotCommand;
import org.structr.schema.SchemaHelper;
import org.structr.schema.importer.RDFImporter;
import org.structr.schema.importer.SchemaAnalyzer;
import org.structr.schema.importer.SchemaJsonImporter;
import org.structr.util.StructrLicenseManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class MaintenanceParameterResource extends Resource {

	private static final Map<String, Class> maintenanceCommandMap = new LinkedHashMap<>();

	static {

		maintenanceCommandMap.put("importRdf", RDFImporter.class);
		maintenanceCommandMap.put("importSchemaJson", SchemaJsonImporter.class);
		maintenanceCommandMap.put("rebuildIndex", BulkRebuildIndexCommand.class);
		maintenanceCommandMap.put("rebuildIndexForType", BulkRebuildIndexCommand.class);
		maintenanceCommandMap.put("createLabels", BulkCreateLabelsCommand.class);
		maintenanceCommandMap.put("clearDatabase", ClearDatabase.class);
		maintenanceCommandMap.put("fixNodeProperties", BulkFixNodePropertiesCommand.class);
		maintenanceCommandMap.put("setNodeProperties", BulkSetNodePropertiesCommand.class);
		maintenanceCommandMap.put("changeNodePropertyKey", BulkChangeNodePropertyKeyCommand.class);
		maintenanceCommandMap.put("setRelationshipProperties", BulkSetRelationshipPropertiesCommand.class);
		maintenanceCommandMap.put("copyRelationshipProperties", BulkCopyRelationshipPropertyCommand.class);
		maintenanceCommandMap.put("createLicense", StructrLicenseManager.CreateLicenseCommand.class);
		maintenanceCommandMap.put("updateLicense", StructrLicenseManager.UpdateLicenseCommand.class);
		maintenanceCommandMap.put("setUuid", BulkSetUuidCommand.class);
		maintenanceCommandMap.put("sync", SyncCommand.class);
		maintenanceCommandMap.put("snapshot", SnapshotCommand.class);
		maintenanceCommandMap.put("flushCaches", FlushCachesCommand.class);
		maintenanceCommandMap.put("analyzeSchema", SchemaAnalyzer.class);
		maintenanceCommandMap.put("migrateChangelog", BulkMigrateChangelogCommand.class);
		maintenanceCommandMap.put("manageDatabases", ManageDatabasesCommand.class);
		maintenanceCommandMap.put("manageThreads", ManageThreadsCommand.class);

	}

	private String uriPart = null;

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		if (maintenanceCommandMap.containsKey(part)) {

			this.uriPart = part;

			return true;

		}

		return false;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed, use POST to run maintenance commands");
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed, use POST to run maintenance commands");
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("POST not allowed here, this should not happen. Please report the URL that led to this error message to team@structr.com. Thank you!");
	}

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {
		return null;
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	public Class getMaintenanceCommand() {
		return maintenanceCommandMap.get(uriPart);
	}

	@Override
	public String getUriPart() {
		return uriPart;
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}

        @Override
        public String getResourceSignature() {
                return SchemaHelper.normalizeEntityName(getUriPart());
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
}
