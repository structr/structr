/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.rest.constraint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.agent.RebuildIndexTask;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;

/**
 *
 * @author Christian Morgner
 */
public class MaintenanceParameterConstraint extends ResourceConstraint {

	private static final Map<String, Class> maintenanceCommandMap = new LinkedHashMap<String, Class>();

	static {
		maintenanceCommandMap.put("rebuildIndex", RebuildIndexTask.class);
	}

	private String uriPart = null;

	public Class getMaintenanceCommand() {
		return maintenanceCommandMap.get(uriPart);
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		if(maintenanceCommandMap.containsKey(part)) {
			this.uriPart = part;
			return true;
		}

		return false;
	}

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {
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
	public RestMethodResult doHead() throws FrameworkException {
		throw new NotAllowedException();
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new NotAllowedException();
	}

	@Override
	public String getUriPart() {
		return uriPart;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws FrameworkException {
		return null;
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}
}
