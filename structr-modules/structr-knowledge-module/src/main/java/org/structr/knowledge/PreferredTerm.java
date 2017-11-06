/**
 * Copyright (C) 2010-2017 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.schema.SchemaService;

/**
 * Base class of a preferred term as defined in ISO 25964
 */

public class PreferredTerm extends ThesaurusTerm {
	
	private static final Logger logger = LoggerFactory.getLogger(PreferredTerm.class.getName());
	
	public static final Property<ThesaurusConcept> preferredLabels = new EndNode<>("preferredLabels", TermHasLabel.class);

	static {

		SchemaService.registerBuiltinTypeOverride("PreferredTerm", PreferredTerm.class.getName());
	}	

}
