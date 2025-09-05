/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.knowledge.iso25964.relationship.SimpleNonPreferredTermUSEPreferredTerm;
import org.structr.knowledge.iso25964.relationship.ThesaurusConcepthasNonPreferredLabelSimpleNonPreferredTerm;

/**
 * Class as defined in ISO 25964 data model
 */
public class SimpleNonPreferredTerm extends ThesaurusTerm {

	public static final Property<ThesaurusConcept> conceptsProperty = new StartNode<>("concepts", ThesaurusConcepthasNonPreferredLabelSimpleNonPreferredTerm.class);
	public static final Property<PreferredTerm> preferredTerm       = new EndNode<>("preferredTerm", SimpleNonPreferredTermUSEPreferredTerm.class);
	public static final Property<Boolean> hiddenProperty            = new BooleanProperty("hidden");

	public static final View uiView = new View(SimpleNonPreferredTerm.class, PropertyView.Ui,
		hiddenProperty
	);
}
