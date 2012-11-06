/*
 *  Copyright (C) 2010-2012 Bastian Knerr
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
package org.structr.rest.resource.visitor;

import org.structr.common.error.FrameworkException;
import org.structr.rest.resource.Resource;
import org.structr.rest.resource.TypeResource;
import org.structr.rest.resource.TypedIdResource;
import org.structr.rest.resource.UuidResource;

/**
 * @author Bastian Knerr
 *
 */
public class TypeResourceVisitorImpl implements ResourceVisitor {

	private final TypeResource resource;
	protected Resource result;

	/**
	 * Constructor.
	 */
	public TypeResourceVisitorImpl(final TypeResource current) {
		resource = current;
	}

	@Override
	public Resource getNext() {
		return result;
	}

	@Override
	public void visit(final UuidResource next) throws FrameworkException {
		final TypedIdResource constraint = new TypedIdResource(resource.getSecurityContext(), next, resource);

		constraint.configureIdProperty(resource.getIdProperty());

		result = constraint;
	}
}
