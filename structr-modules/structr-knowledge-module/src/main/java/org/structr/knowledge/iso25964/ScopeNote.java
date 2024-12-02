/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.knowledge.iso25964;

import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.knowledge.iso25964.relationship.ThesaurusConcepthasScopeNoteScopeNote;

/**
 * Class as defined in ISO 25964 data model
 */
public class ScopeNote extends Note {

	public static final Property<ThesaurusConcept> conceptProperty = new StartNode<>("concept", ThesaurusConcepthasScopeNoteScopeNote.class);
}
