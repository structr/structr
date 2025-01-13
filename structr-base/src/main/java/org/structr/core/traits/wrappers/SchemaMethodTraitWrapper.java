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
package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionEntry;

import java.util.List;
import java.util.Map;

public class SchemaMethodTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements SchemaMethod {

	public SchemaMethodTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public AbstractSchemaNode getSchemaNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("schemaNode"));
		if (node != null) {

			return node.as(AbstractSchemaNode.class);
		}

		return null;
	}

	@Override
	public Iterable<SchemaMethodParameter> getParameters() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("parameters");

		return Iterables.map(n -> n.as(SchemaMethodParameter.class), wrappedObject.getProperty(key));
	}

	@Override
	public String getName() {
		return wrappedObject.getProperty(traits.key("name"));
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key("source"));
	}

	@Override
	public String getSummary() {
		return wrappedObject.getProperty(traits.key("summary"));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key("description"));
	}

	@Override
	public String getCodeType() {
		return wrappedObject.getProperty(traits.key("codeType"));
	}

	@Override
	public String getReturnType() {
		return wrappedObject.getProperty(traits.key("returnType"));
	}

	@Override
	public String getOpenAPIReturnType() {
		return wrappedObject.getProperty(traits.key("openAPIReturnType"));
	}

	@Override
	public String getVirtualFileName() {
		return wrappedObject.getProperty(traits.key("virtualFileName"));
	}

	@Override
	public String getSignature() {
		return wrappedObject.getProperty(traits.key("signature"));
	}

	@Override
	public String[] getExceptions() {
		return wrappedObject.getProperty(traits.key("exceptions"));
	}

	@Override
	public String[] getTags() {
		return wrappedObject.getProperty(traits.key("tags"));
	}

	@Override
	public boolean callSuper() {
		return wrappedObject.getProperty(traits.key("callSuper"));
	}

	@Override
	public boolean overridesExisting() {
		return wrappedObject.getProperty(traits.key("overridesExisting"));
	}

	@Override
	public boolean doExport() {
		return wrappedObject.getProperty(traits.key("doExport"));
	}

	@Override
	public boolean includeInOpenAPI() {
		return wrappedObject.getProperty(traits.key("includeInOpenAPI"));
	}

	@Override
	public ActionEntry getActionEntry(Map<String, SchemaNode> schemaNodes, AbstractSchemaNode schemaEntity) throws FrameworkException {
		return null;
	}

	@Override
	public boolean isStaticMethod() {
		return wrappedObject.getProperty(traits.key("isStatic"));
	}

	@Override
	public boolean isPrivateMethod() {
		return wrappedObject.getProperty(traits.key("isPrivate"));
	}

	@Override
	public boolean returnRawResult() {
		return wrappedObject.getProperty(traits.key("returnRawResult"));
	}

	@Override
	public String getHttpVerb() {
		return wrappedObject.getProperty(traits.key("httpVerb"));
	}

	public SchemaMethodParameter getSchemaMethodParameter(final String name) {

		for (final SchemaMethodParameter param : getParameters()) {

			if (name.equals(param.getName())) {
				return param;
			}
		}

		return null;
	}

	@Override
	public boolean isJava() {
		return "java".equals(getCodeType());
	}

	@Override
	public boolean isLifecycleMethod() {

		final AbstractSchemaNode parent = getSchemaNode();
		final boolean hasParent         = (parent != null);
		final String methodName         = getName();

		if (hasParent) {

			final List<String> typeBasedLifecycleMethods = List.of("onNodeCreation", "onCreate", "afterCreate", "onSave", "afterSave", "onDelete", "afterDelete");
			final List<String> fileLifecycleMethods      = List.of("onUpload", "onDownload");
			final List<String> userLifecycleMethods      = List.of("onOAuthLogin");

			for (final String lifecycleMethodPrefix : typeBasedLifecycleMethods) {

				if (methodName.startsWith(lifecycleMethodPrefix)) {
					return true;
				}
			}

			boolean inheritsFromFile = false;
			boolean inheritsFromUser = false;

			final Traits traits = Traits.of(parent.getName());
			if (traits != null) {

				inheritsFromFile = traits.contains("AbstractFile");
				inheritsFromUser = traits.contains("User");
			}

			if (inheritsFromFile) {

				for (final String lifecycleMethodName : fileLifecycleMethods) {

					if (methodName.equals(lifecycleMethodName)) {
						return true;
					}
				}
			}

			if (inheritsFromUser) {

				for (final String lifecycleMethodName : userLifecycleMethods) {

					if (methodName.equals(lifecycleMethodName)) {
						return true;
					}
				}
			}

		} else {

			final List<String> globalLifecycleMethods = List.of("onStructrLogin", "onStructrLogout", "onAcmeChallenge");

			for (final String lifecycleMethodName : globalLifecycleMethods) {

				if (methodName.equals(lifecycleMethodName)) {
					return true;
				}
			}

		}

		return false;
	}
}
