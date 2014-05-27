/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import org.structr.core.graph.BulkCopyRelationshipPropertyCommand;
import org.structr.core.graph.BulkSetRelationshipPropertiesCommand;
import org.structr.core.graph.BulkFixNodePropertiesCommand;
import org.structr.core.Result;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.BulkSetNodePropertiesCommand;
import org.structr.core.graph.ClearDatabase;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.property.PropertyKey;
import org.structr.core.Command;
import org.structr.agent.Task;
import org.structr.core.graph.BulkChangeNodePropertyKeyCommand;
import org.structr.core.graph.BulkCreateLabelsCommand;
import org.structr.core.graph.BulkDeleteSoftDeletedNodesCommand;
import org.structr.core.graph.BulkRebuildIndexCommand;
import org.structr.core.graph.BulkSetUuidCommand;
import org.structr.core.graph.SyncCommand;
import org.structr.schema.SchemaHelper;
import org.structr.schema.importer.GraphGistImporter;
import org.structr.schema.importer.RDFImporter;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class MaintenanceParameterResource extends Resource {

	private static final Map<String, Class> maintenanceCommandMap = new LinkedHashMap<>();

	//~--- static initializers --------------------------------------------

	static {

		maintenanceCommandMap.put("importGist", GraphGistImporter.class);
		maintenanceCommandMap.put("importRdf", RDFImporter.class);
		maintenanceCommandMap.put("rebuildIndex", BulkRebuildIndexCommand.class);
		maintenanceCommandMap.put("rebuildIndexForType", BulkRebuildIndexCommand.class);
		maintenanceCommandMap.put("createLabels", BulkCreateLabelsCommand.class);
		maintenanceCommandMap.put("clearDatabase", ClearDatabase.class);
		maintenanceCommandMap.put("fixNodeProperties", BulkFixNodePropertiesCommand.class);
		maintenanceCommandMap.put("setNodeProperties", BulkSetNodePropertiesCommand.class);
		maintenanceCommandMap.put("changeNodePropertyKey", BulkChangeNodePropertyKeyCommand.class);
		maintenanceCommandMap.put("setRelationshipProperties", BulkSetRelationshipPropertiesCommand.class);
		maintenanceCommandMap.put("copyRelationshipProperties", BulkCopyRelationshipPropertyCommand.class);
		maintenanceCommandMap.put("deleteSoftDeletedNodes", BulkDeleteSoftDeletedNodesCommand.class);
		maintenanceCommandMap.put("setUuid", BulkSetUuidCommand.class);
		maintenanceCommandMap.put("sync", SyncCommand.class);

	}

	//~--- fields ---------------------------------------------------------

	private String uriPart = null;

	//~--- methods --------------------------------------------------------

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
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {
		throw new NotAllowedException();
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException();
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return null;
	}

	//~--- get methods ----------------------------------------------------

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

	public static void registerMaintenanceTask(String key, Class<? extends Task> task) {

		if(maintenanceCommandMap.containsKey(key)) {
			throw new IllegalStateException("Maintenance command for key " + key + " already registered!");
		}

		maintenanceCommandMap.put(key, task);
	}

	public static void registerMaintenanceCommand(String key, Class<? extends Command> command) {

		if(maintenanceCommandMap.containsKey(key)) {
			throw new IllegalStateException("Maintenance command for key " + key + " already registered!");
		}

		maintenanceCommandMap.put(key, command);
	}
}
