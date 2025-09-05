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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.knowledge.iso25964.relationship.NoterefersToThesaurusConcept;

import java.util.Date;

/**
 * Class as defined in ISO 25964 data model
 */
public class Note extends AbstractNode {

	public static final Property<Iterable<ThesaurusConcept>> conceptsProperty = new EndNodes<>("concepts", NoterefersToThesaurusConcept.class);

	public static final Property<String> lexicalValueProperty = new StringProperty("lexicalValue").indexed().notNull();
	public static final Property<Date> createdProperty        = new DateProperty("created");
	public static final Property<Date> modifiedProperty       = new DateProperty("modified");
	public static final Property<String> langProperty         = new EnumProperty("lang", ThesaurusTerm.Lang.class);

	public static final View uiView = new View(Note.class, PropertyView.Ui,
		lexicalValueProperty, createdProperty, modifiedProperty, langProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, Note.lexicalValueProperty, errorBuffer);

		return valid;
	}
}
