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
package org.structr.transform;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.transform.relationship.VirtualTypevirtualPropertyVirtualProperty;

/**
 *
 */
public class VirtualProperty extends AbstractNode {

	public static final Property<VirtualType> virtualType = new StartNode<>("virtualType", VirtualTypevirtualPropertyVirtualProperty.class);
	public static final Property<Integer> position        = new IntProperty("position").indexed();
	public static final Property<String> sourceName       = new StringProperty("sourceName");
	public static final Property<String> targetName       = new StringProperty("targetName");
	public static final Property<String> inputFunction    = new StringProperty("inputFunction");
	public static final Property<String> outputFunction   = new StringProperty("outputFunction");

	public static final View defaultView = new View(VirtualProperty.class, PropertyView.Public,
		virtualType, sourceName, targetName, inputFunction, outputFunction, position
	);

	public static final View uiView = new View(VirtualProperty.class, PropertyView.Ui,
		virtualType, sourceName, targetName, inputFunction, outputFunction, position
	);

	public Integer getPosition() {
		return getProperty(position);
	}

	public String getSourceName() {
		return getProperty(sourceName);
	}

	public String getTargetName() {
		return getProperty(targetName);
	}

	public String getOutputFunction() {
		return getProperty(outputFunction);
	}

	public String getInputFunction() {
		return getProperty(inputFunction);
	}

	public Transformation getTransformation(final Class _type) throws FrameworkException {

		final String _sourceName     = getSourceName();
		final String _inputFunction  = getInputFunction();
		final String _outputFunction = getOutputFunction();
		String _targetName           = getTargetName();

		if (_sourceName == null && _outputFunction == null) {
			throw new FrameworkException(500, "VirtualProperty with ID " + getUuid() + " needs source name or output function");
		}

		// don't rename
		if (_targetName == null) {
			_targetName = _sourceName;
		}

		return new Transformation(_type, _sourceName, _targetName, _inputFunction, _outputFunction);
	}
}
