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

import org.structr.rest.ResourceConstraintProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The default constraint provider for structr.
 *
 * @author Christian Morgner
 */
public class StructrConstraintProvider implements ResourceConstraintProvider {

	@Override
	public Map<Pattern, Class<? extends ResourceConstraint>> getConstraints() {

		Map<Pattern, Class<? extends ResourceConstraint>> constraintMap = new LinkedHashMap<Pattern, Class<? extends ResourceConstraint>>();

		constraintMap.put(Pattern.compile("[0-9]+"),		IdConstraint.class);			// this matches the ID constraint first

		constraintMap.put(Pattern.compile("in"),		RelationshipConstraint.class);		// incoming relationship
		constraintMap.put(Pattern.compile("out"),		RelationshipConstraint.class);		// outgoing relationship
		constraintMap.put(Pattern.compile("start"),		RelationshipNodeConstraint.class);	// start node
		constraintMap.put(Pattern.compile("end"),		RelationshipNodeConstraint.class);	// end node

		// FIXME: are views needed here?
		constraintMap.put(Pattern.compile("public"),		ViewFilterConstraint.class);		// public view (default)
		constraintMap.put(Pattern.compile("protected"),		ViewFilterConstraint.class);		// protected view
		constraintMap.put(Pattern.compile("private"),		ViewFilterConstraint.class);		// private view
		constraintMap.put(Pattern.compile("owner"),		ViewFilterConstraint.class);		// owner view
		constraintMap.put(Pattern.compile("admin"),		ViewFilterConstraint.class);		// admin view
		constraintMap.put(Pattern.compile("all"),		ViewFilterConstraint.class);		// all view

		constraintMap.put(Pattern.compile("[a-z_]+"),		TypeConstraint.class);			// any type match

		return constraintMap;
	}
}
