/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.fulltext;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.fulltext.relationship.IndexableINDEXED_WORDIndexedWord;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;

/**
 */
public class IndexedWord extends AbstractNode {

	Property<Iterable<Indexable>> indexablesProperty  = new StartNodes<>("indexables", IndexableINDEXED_WORDIndexedWord.class);
	public static final Property<String> nameProperty = new StringProperty("name").indexed().unique();

	public static final View defaultView = new View(IndexedWord.class, PropertyView.Public,
		nameProperty
	);

	public static final View uiView      = new View(IndexedWord.class, PropertyView.Ui,
		nameProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidUniqueProperty(this, IndexedWord.nameProperty, errorBuffer);

		return valid;
	}
}
