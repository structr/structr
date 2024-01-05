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

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;

/**
 *
 */
public interface VirtualProperty extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("VirtualProperty");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/VirtualProperty"));

		type.addIntegerProperty("position",      PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("sourceName",     PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("targetName",     PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("inputFunction",  PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("outputFunction", PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("position",       Integer.class);
		type.addPropertyGetter("sourceName",     String.class);
		type.addPropertyGetter("targetName",     String.class);
		type.addPropertyGetter("inputFunction",  String.class);
		type.addPropertyGetter("outputFunction", String.class);

		type.overrideMethod("getTransformation", false, "return " + VirtualProperty.class.getName() + ".getTransformation(this, arg0);");

		// view configuration
		type.addViewProperty(PropertyView.Public, "virtualType");
		type.addViewProperty(PropertyView.Ui, "virtualType");
	}}

	Transformation getTransformation(final Class arg0) throws FrameworkException;
	Integer getPosition();
	String getSourceName();
	String getInputFunction();
	String getOutputFunction();
	String getTargetName();

	/*

	public static final Property<VirtualType> virtualType = new StartNode<>("virtualType", VirtualTypeProperty.class);
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
	*/

	static Transformation getTransformation(final VirtualProperty thisProperty, final Class _type) throws FrameworkException {

		final String _sourceName     = thisProperty.getSourceName();
		final String _inputFunction  = thisProperty.getInputFunction();
		final String _outputFunction = thisProperty.getOutputFunction();
		String _targetName           = thisProperty.getTargetName();

		if (_sourceName == null && _outputFunction == null) {
			throw new FrameworkException(500, "VirtualProperty with ID " + thisProperty.getUuid() + " needs source name or output function");
		}

		// don't rename
		if (_targetName == null) {
			_targetName = _sourceName;
		}

		return new Transformation(_type, _sourceName, _targetName, _inputFunction, _outputFunction);
	}
}
