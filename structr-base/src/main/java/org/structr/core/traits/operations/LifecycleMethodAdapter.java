/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.traits.operations;

import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.ScriptMethod;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.graphobject.*;
import org.structr.core.traits.operations.nodeinterface.OnNodeCreation;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.schema.action.Actions;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LifecycleMethodAdapter implements OnCreation, OnModification, OnDeletion, AfterCreation, AfterModification, AfterDeletion, OnNodeCreation, OnNodeDeletion {

	private final List<ScriptMethod> methods = new LinkedList<>();

	public LifecycleMethodAdapter(final SchemaMethod schemaMethod) {
		this.methods.add(new ScriptMethod(schemaMethod));
	}

	public void addMethod(final SchemaMethod schemaMethod) {
		this.methods.add(new ScriptMethod(schemaMethod));
	}

	@Override
	public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final ScriptMethod method : methods) {
			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
					.currentMethod(method)
					.build();

			Actions.execute(securityContext, graphObject, "${" + method.getRawSource().trim() + "}", null, type + ".onCreate", null, scriptConfig);
		}
	}

	@Override
	public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final ScriptMethod method : methods) {
			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
					.currentMethod(method)
					.build();

			Actions.execute(securityContext, graphObject, "${" + method.getRawSource().trim() + "}", null, type + ".afterCreate", null, scriptConfig);
		}
	}

	@Override
	public void afterDeletion(final GraphObject graphObject, final SecurityContext securityContext, final PropertyMap properties) {

		final String type = graphObject.getTraits().getName();

		try {

			securityContext.getContextStore().setConstant("data", properties.getAsMap());

			// entity is null because it is deleted, properties are available via "data" keyword
			for (final ScriptMethod method : methods) {
				final ScriptConfig scriptConfig = ScriptConfig.builder()
						.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
						.currentMethod(method)
						.build();

				Actions.execute(securityContext, null, "${" + method.getRawSource().trim() + "}", null, type + ".afterDelete", null, scriptConfig);
			}

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void afterModification(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final ScriptMethod method : methods) {
			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
					.currentMethod(method)
					.build();

			Actions.execute(securityContext, graphObject, "${" + method.getRawSource().trim() + "}", null, type + ".afterSave", null, scriptConfig);
		}
	}

	@Override
	public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final ScriptMethod method : methods) {
			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
					.currentMethod(method)
					.build();

			Actions.execute(securityContext, graphObject, "${" + method.getRawSource().trim() + "}", null, type + ".onDelete", null, scriptConfig);
		}
	}

	@Override
	public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		final String type = graphObject.getTraits().getName();

		for (final ScriptMethod method : methods) {
			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
					.currentMethod(method)
					.build();

			final Map<String, Object> parameters = new LinkedHashMap<>();
			parameters.put("modifications", modificationQueue.getModifications(graphObject));

			Actions.execute(securityContext, graphObject, "${" + method.getRawSource().trim() + "}", parameters, type + ".onSave", null, scriptConfig);
		}
	}

	@Override
	public void onNodeCreation(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

		final String type = nodeInterface.getTraits().getName();

		for (final ScriptMethod method : methods) {
			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
					.currentMethod(method)
					.build();

			Actions.execute(securityContext, nodeInterface, "${" + method.getRawSource().trim() + "}", null, type + ".onNodeCreation", null, scriptConfig);
		}
	}

	@Override
	public void onNodeDeletion(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

		final String type = nodeInterface.getTraits().getName();

		for (final ScriptMethod method : methods) {
			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
					.currentMethod(method)
					.build();

			Actions.execute(securityContext, nodeInterface, "${" + method.getRawSource().trim() + "}", null, type + ".onNodeDeletion", null, scriptConfig);
		}
	}
}
