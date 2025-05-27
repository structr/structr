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
import org.structr.knowledge.iso25964.relationship.ThesaurushasVersionVersionHistory;

import java.util.Date;

/**
 * Base class of a concept group as defined in ISO 25964
 */
public class VersionHistory extends AbstractNode {

	public static final Property<Thesaurus> thesaurusProperty    = new StartNode<>("thesaurus", ThesaurushasVersionVersionHistory.class);
	public static final Property<String> identifierProperty      = new StringProperty("identifier").indexed();
	public static final Property<Date> dateProperty              = new DateProperty("date");
	public static final Property<String> versionNoteProperty     = new StringProperty("versionNote");
	public static final Property<Boolean> currentVersionProperty = new BooleanProperty("currentVersion");
	public static final Property<Boolean> thisVersionProperty    = new BooleanProperty("thisVersion").notNull();

	public static final View uiView = new View(VersionHistory.class, PropertyView.Ui,
		identifierProperty, dateProperty, versionNoteProperty, currentVersionProperty, thisVersionProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, VersionHistory.thisVersionProperty, errorBuffer);

		return valid;
	}
}
