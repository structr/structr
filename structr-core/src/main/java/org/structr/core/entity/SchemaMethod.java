/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.core.entity;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.SchemaMethodParameters;
import org.structr.core.entity.relationship.SchemaNodeMethod;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.Schema;
import org.structr.schema.action.ActionEntry;

/**
 *
 *
 */
public class SchemaMethod extends SchemaReloadingNode implements Favoritable {

	public static final Property<List<SchemaMethodParameter>> parameters = new EndNodes<>("parameters", SchemaMethodParameters.class);
	public static final Property<AbstractSchemaNode> schemaNode          = new StartNode<>("schemaNode", SchemaNodeMethod.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));
	public static final Property<String>             virtualFileName     = new StringProperty("virtualFileName");
	public static final Property<String>             returnType          = new StringProperty("returnType");
	public static final Property<String>             source              = new StringProperty("source");
	public static final Property<String>             comment             = new StringProperty("comment");
	public static final Property<String[]>           exceptions          = new ArrayProperty("exceptions", String.class);
	public static final Property<Boolean>            callSuper           = new BooleanProperty("callSuper");
	public static final Property<Boolean>            overridesExisting   = new BooleanProperty("overridesExisting");
	public static final Property<String>             codeType            = new StringProperty("codeType");

	public static final View defaultView = new View(SchemaMethod.class, PropertyView.Public,
		name, schemaNode, source, comment, isFavoritable
	);

	public static final View uiView = new View(SchemaMethod.class, PropertyView.Ui,
		name, schemaNode, source, comment, isFavoritable
	);

	public static final View exportView = new View(SchemaMethod.class, "export",
		id, type, schemaNode, name, source, comment
	);

	public ActionEntry getActionEntry(final Schema schemaEntity) {

		final ActionEntry entry  = new ActionEntry("___" + getProperty(AbstractNode.name), getProperty(SchemaMethod.source), getProperty(SchemaMethod.codeType));

		for (final SchemaMethodParameter parameter : getProperty(parameters)) {

			entry.addParameter(parameter.getParameterType(), parameter.getName());
		}

		entry.setReturnType(getProperty(returnType));
		entry.setCallSuper(getProperty(callSuper));

		final String[] _exceptions = getProperty(exceptions);
		if (_exceptions != null) {

			for (final String exception : _exceptions) {
				entry.addException(exception);
			}
		}

		// check for overridden methods and determine method signature etc. from superclass(es)
		if (getProperty(overridesExisting)) {
			determineSignature(schemaEntity, entry, getProperty(name));
		}

		return entry;
	}

	public boolean isJava() {
		return "java".equals(getProperty(codeType));
	}

	// ----- private methods -----
	private void determineSignature(final Schema schemaEntity, final ActionEntry entry, final String methodName) {

		// try to find a method with a signature in one of the superclasses
		final String superclass = schemaEntity.getSuperclassName();
		final App app           = StructrApp.getInstance();

		if (superclass != null && !AbstractNode.class.getSimpleName().equals(superclass)) {

			try {

				// try to find superclass in schema nodes
				final SchemaNode _schemaNode = app.nodeQuery(SchemaNode.class).andName(superclass).getFirst();
				if (_schemaNode != null) {

					final SchemaMethod superMethod = app.nodeQuery(SchemaMethod.class)
						.and(SchemaMethod.schemaNode, _schemaNode)
						.and(SchemaMethod.name, methodName)
						.getFirst();

					if (superMethod != null) {

						final ActionEntry superEntry = superMethod.getActionEntry(_schemaNode);

						entry.copy(superEntry);

					}
				}

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

		} else {

			// superclass is AbstractNode
			for (final Method method : AbstractNode.class.getMethods()) {

				if (methodName.equals(method.getName())) {

					final Type returnType = method.getGenericReturnType();

					// check for generic return type, and if the method defines its own generic type
					if (returnType instanceof TypeVariable && ((TypeVariable)returnType).getGenericDeclaration().equals(method)) {

						// method defines its own generic type
						entry.setReturnType("<" + returnType.getTypeName() + "> " + returnType.getTypeName());

					} else {

						// non-generic return type
						entry.setReturnType(returnType.getTypeName());
					}

					for (final Parameter parameter : method.getParameters()) {

						final String type = parameter.getParameterizedType().getTypeName();
						final String name = parameter.getType().getSimpleName();

						// convention: parameter name is lowercase type name
						entry.addParameter(type, StringUtils.uncapitalize(name));
					}

					for (final Class exception : method.getExceptionTypes()) {
						entry.addException(exception.getName());
					}

					entry.setOverrides(getProperty(overridesExisting));
					entry.setCallSuper(getProperty(callSuper));
					break;
				}
			}
		}
	}

	// ----- interface Favoritable -----
	@Override
	public String getContext() {

		final AbstractSchemaNode parent = getProperty(SchemaMethod.schemaNode);
		final StringBuilder buf = new StringBuilder();

		if (parent != null) {

			buf.append(parent.getProperty(SchemaNode.name));
			buf.append(".");
			buf.append(getProperty(name));
		}

		return buf.toString();
	}

	@Override
	public String getFavoriteContent() {
		return getProperty(SchemaMethod.source);
	}

	@Override
	public String getFavoriteContentType() {
		return "application/x-structr-javascript";
	}

	@Override
	public void setFavoriteContent(String content) throws FrameworkException {
		setProperty(SchemaMethod.source, content);
	}
}
