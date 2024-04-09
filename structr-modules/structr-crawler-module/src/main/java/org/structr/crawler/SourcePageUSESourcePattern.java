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
package org.structr.crawler;

import org.structr.common.View;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.property.Property;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;

public class SourcePageUSESourcePattern extends OneToMany<SourcePage, SourcePattern> {

	public static final Property<java.lang.String> sourceIdProperty = new SourceId("sourceId");
	public static final Property<java.lang.String> targetIdProperty = new TargetId("targetId");

	public static final View uiView = new View(SourcePageUSESourcePattern.class, "ui",
		sourceIdProperty, targetIdProperty
	);

	@Override
	public Class<SourcePage> getSourceType() {
		return SourcePage.class;
	}

	@Override
	public Class<SourcePattern> getTargetType() {
		return SourcePattern.class;
	}

	@Override
	public Property<java.lang.String> getSourceIdProperty() {
		return sourceId;
	}

	@Override
	public Property<java.lang.String> getTargetIdProperty() {
		return targetId;
	}

	@Override
	public java.lang.String name() {
		return "USE";
	}


	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}

}