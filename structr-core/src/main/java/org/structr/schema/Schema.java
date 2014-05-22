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
package org.structr.schema;

import java.util.Set;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public interface Schema {

	public String getSource(final ErrorBuffer errorBuffer) throws FrameworkException;
	public String getMultiplicity(final String propertyNameToCheck);
	public String getRelatedType(final String propertyNameToCheck);
	public PropertyContainer getPropertyContainer();
	public Set<String> getViews();
	public String getClassName();
}
