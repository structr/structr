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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipIdConstraint extends FilterableConstraint {

	private static final Logger logger = Logger.getLogger(TypedIdConstraint.class.getName());

	protected RelationshipConstraint relationshipConstraint = null;
	protected IdConstraint idConstraint = null;

	protected RelationshipIdConstraint(SecurityContext securityContext) {
		this.securityContext = securityContext;
		// empty protected constructor
	}

	public RelationshipIdConstraint(SecurityContext securityContext, RelationshipConstraint relationshipConstraint, IdConstraint idConstraint) {
		this.securityContext = securityContext;
		this.relationshipConstraint = relationshipConstraint;
		this.idConstraint = idConstraint;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public List<GraphObject> doGet(List<VetoableGraphObjectListener> listeners) throws PathException {

		List<? extends GraphObject> results = relationshipConstraint.doGet(listeners);
		long desiredId = idConstraint.getId();
		GraphObject desiredObject = null;

		for(GraphObject obj : results) {
			if(obj.getId() == desiredId) {
				desiredObject = obj;
				break;
			}
		}

		// if object was found, return it
		if(desiredObject != null) {
			List<GraphObject> resultList = new LinkedList<GraphObject>();
			resultList.add(desiredObject);

			return resultList;
		}

		throw new NotFoundException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet, List<VetoableGraphObjectListener> listeners) throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doHead() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public RelationshipConstraint getRelationshipConstraint() {
		return relationshipConstraint;
	}

	public IdConstraint getIdConstraint() {
		return idConstraint;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof RelationshipNodeConstraint) {

			((RelationshipNodeConstraint)next).wrapConstraint(this);
			return next;
		}

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return relationshipConstraint.getUriPart().concat("/").concat(idConstraint.getUriPart());
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}
}
