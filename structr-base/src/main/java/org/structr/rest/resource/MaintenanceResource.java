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
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.agent.Task;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.SystemException;

import java.util.Collections;
import java.util.Map;

/**
 * A resource constraint that allows to execute maintenance tasks via
 * REST API.
 *
 *
 */
public class MaintenanceResource extends Resource {

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceResource.class.getName());

	private String taskOrCommandName = null;
	private Class taskOrCommand      = null;

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		return ("maintenance".equals(part));
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if ((securityContext != null) && isSuperUser()) {

			if (this.taskOrCommand != null) {

				RuntimeEventLog.maintenance(taskOrCommand.getSimpleName(), propertySet);

				try {

					final App app = StructrApp.getInstance(securityContext);

					if (Task.class.isAssignableFrom(taskOrCommand)) {

						final Task task = (Task) taskOrCommand.newInstance();

						app.processTasks(task);

					} else if (MaintenanceCommand.class.isAssignableFrom(taskOrCommand)) {

						final MaintenanceCommand cmd = (MaintenanceCommand)StructrApp.getInstance(securityContext).command(taskOrCommand);

						// flush caches if required
						if (cmd.requiresFlushingOfCaches()) {

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

				} catch (InstantiationException iex) {
					throw new SystemException(iex.getMessage());
				} catch (IllegalAccessException iaex) {
					throw new SystemException(iaex.getMessage());
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
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof MaintenanceParameterResource) {

			final MaintenanceParameterResource param = ((MaintenanceParameterResource) next);
			this.taskOrCommandName = param.getUriPart();

			this.taskOrCommand = param.getMaintenanceCommand();

			return this;
		}

		if(next instanceof SchemaJsonResource) {
			return next;
		}

		// accept global schema methods resource as successor
		if (next instanceof GlobalSchemaMethodsResource) {
			return next;
		}

		return null;
	}

	@Override
	public boolean createPostTransaction() {
		return false;
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	@Override
	public String getUriPart() {
		return "maintenance";
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

        @Override
        public String getResourceSignature() {
                return getUriPart();
        }

	// ----- private methods -----
	private boolean isSuperUser() throws FrameworkException {

		try (final Tx tx = StructrApp.getInstance().tx()) {
			return securityContext.isSuperUser();
		}
	}
}
