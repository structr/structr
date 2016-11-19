/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.entity.relation;

import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.ManyToMany;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.AbstractMinifiedFile;
import org.structr.web.entity.FileBase;

public class MinificationSource extends ManyToMany<AbstractMinifiedFile, FileBase> {

	public static final Property<Integer> position = new IntProperty("position").defaultValue(0).indexed();

	public static final View uiView = new View(MinificationSource.class, PropertyView.Ui, position);

	@Override
	public Class<AbstractMinifiedFile> getSourceType() {
		return AbstractMinifiedFile.class;
	}

	@Override
	public Class<FileBase> getTargetType() {
		return FileBase.class;
	}

	@Override
	public String name() {
		return "MINIFICATION";
	}

	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public void onRelationshipCreation() {

		try {

			setProperties(securityContext, new PropertyMap(position, getSourceNode().getMaxPosition() + 1));

		} catch (FrameworkException ex) {

			LoggerFactory.getLogger(MinificationSource.class.getName()).error("Failed setting minification position!", ex);

		}

	}
}
