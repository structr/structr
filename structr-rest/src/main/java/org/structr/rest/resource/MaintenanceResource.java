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

import org.structr.core.Result;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.agent.Task;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.SystemException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * A resource constraint that allows to execute maintenance tasks via
 * REST API.
 *
 * @author Christian Morgner
 */
public class MaintenanceResource extends Resource {

	private static final Logger logger = Logger.getLogger(MaintenanceResource.class.getName());

	//~--- fields ---------------------------------------------------------

	private Class taskOrCommand = null;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		return ("maintenance".equals(part));
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

		if ((securityContext != null) && isSuperUser()) {

			if (this.taskOrCommand != null) {

				try {

					final App app = StructrApp.getInstance(securityContext);

					if (Task.class.isAssignableFrom(taskOrCommand)) {

						Task task = (Task) taskOrCommand.newInstance();

						app.processTasks(task);

					} else if (MaintenanceCommand.class.isAssignableFrom(taskOrCommand)) {

						MaintenanceCommand cmd = (MaintenanceCommand)StructrApp.getInstance(securityContext).command(taskOrCommand);
						if (cmd.requiresEnclosingTransaction()) {

							try (final Tx tx = app.tx()) {

								cmd.execute(propertySet);
								tx.success();
							}

						} else {

							cmd.execute(propertySet);
						}

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

				throw new NotFoundException();

			}

		} else {

			// loggin disabled because of possible NotInTransactionException
//			logger.log(Level.INFO, "SecurityContext is {0}, user is {1}", new Object[] { (securityContext != null)
//				? "non-null"
//				: "null", ((securityContext != null) && (securityContext.getUser(true) != null))
//					  ? securityContext.getUser(true).getProperty(AbstractNode.name)
//					  : "null" });

			throw new NotAllowedException();

		}
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof MaintenanceParameterResource) {

			this.taskOrCommand = ((MaintenanceParameterResource) next).getMaintenanceCommand();

			return this;

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
		return false;
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
