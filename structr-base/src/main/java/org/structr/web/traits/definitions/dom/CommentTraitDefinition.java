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
package org.structr.web.traits.definitions.dom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Comment;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.operations.Render;
import org.structr.web.traits.wrappers.dom.CommentTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class CommentTraitDefinition extends AbstractNodeTraitDefinition {

	public CommentTraitDefinition() {
		super("Comment");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final Traits traits = graphObject.getTraits();

					graphObject.setProperty(traits.key("contentType"), "text/html");
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			Render.class,
			new Render() {

				@Override
				public void render(final DOMNode thisNode, final RenderContext renderContext, final int depth) throws FrameworkException {

					final Comment comment = thisNode.as(Comment.class);
					String _content       = comment.getContent();

					// Avoid rendering existing @structr comments since those comments are
					// created depending on the visibility settings of individual nodes. If
					// those comments are rendered, there will be duplicates in a round-
					// trip export/import test.
					if (!_content.contains("@structr:")) {

						try {

							final SecurityContext securityContext = thisNode.getSecurityContext();
							final RenderContext.EditMode edit     = renderContext.getEditMode(securityContext.getUser(false));
							final AsyncBuffer buf                 = renderContext.getBuffer();

							if (RenderContext.EditMode.DEPLOYMENT.equals(edit)) {

								thisNode.renderDeploymentExportComments(buf, true);

								buf.append("<!--").append(DOMNode.escapeForHtml(_content)).append("-->");

							} else {

								final PropertyKey<String> key = thisNode.getTraits().key("content");

								_content = thisNode.getPropertyWithVariableReplacement(renderContext, key);

								buf.append("<!--").append(_content).append("-->");
							}

						} catch (Throwable t) {

							// catch exception to prevent ugly status 500 error pages in frontend.
							final Logger logger = LoggerFactory.getLogger(Content.class);
							logger.error("", t);
						}
					}
				}
			}
		);
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(

			Comment.class, (traits, node) -> new CommentTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}