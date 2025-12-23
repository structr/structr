/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;
import org.structr.web.traits.operations.RenderContent;
import org.structr.web.traits.wrappers.dom.TemplateTraitWrapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Documentation(name="Template", type=ConceptType.SystemType, shortDescription="Templates are special elements that may be inserted into a page's DOM tree at any position.", synonyms={"Template element", "Main Page Template"}, parent="System types")
public class TemplateTraitDefinition extends AbstractNodeTraitDefinition {

	public TemplateTraitDefinition() {
		super(StructrTraits.TEMPLATE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			RenderContent.class,
			new RenderContent() {

				@Override
				public void renderContent(final DOMNode node, final RenderContext renderContext, final int depth) throws FrameworkException {

					final SecurityContext securityContext = node.getSecurityContext();
					final EditMode editMode               = renderContext.getEditMode(securityContext.getUser(false));
					final Traits traits                   = node.getTraits();

					if (EditMode.DEPLOYMENT.equals(editMode)) {

						final DOMNode _syncedNode = node.getSharedComponent();
						final AsyncBuffer out     = renderContext.getBuffer();

						if (depth > 0) {
							out.append(DOMNode.indent(depth, renderContext));
						}

						node.renderDeploymentExportComments(out, true);

						out.append("<structr:template src=\"");

						if (_syncedNode != null) {

							// use name of synced node
							final String _name = _syncedNode.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
							out.append(_name != null ? _name.concat("-").concat(_syncedNode.getUuid()) : _syncedNode.getUuid());

						} else {

							// use name of local template
							final String _name = node.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
							out.append(_name != null ? _name.concat("-").concat(node.getUuid()) : node.getUuid());
						}

						out.append("\"");

						node.renderSharedComponentConfiguration(out, editMode);
						node.renderCustomAttributes(out, securityContext, renderContext); // include custom attributes in templates as well!

						out.append(">");

						// fetch children
						final List<RelationshipInterface> rels = node.getChildRelationships();
						if (rels.isEmpty()) {

							// No child relationships, maybe this node is in sync with another node
							if (_syncedNode != null) {
								rels.addAll(_syncedNode.getChildRelationships());
							}
						}

						for (final RelationshipInterface rel : rels) {

							final DOMNode subNode = rel.getTargetNode().as(DOMNode.class);
							subNode.render(renderContext, depth + 1);
						}

						out.append(DOMNode.indent(depth, renderContext));
						out.append("</structr:template>");
						out.append(DOMNode.indent(depth-1, renderContext));

					} else {

						// "super" call using static method..
						getSuper().renderContent(node, renderContext, depth);
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
			Template.class, (traits, node) -> new TemplateTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {
		return Set.of();
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					ContentTraitDefinition.CONTENT_PROPERTY, ContentTraitDefinition.CONTENT_TYPE_PROPERTY,
					DOMNodeTraitDefinition.CHILDREN_PROPERTY, DOMNodeTraitDefinition.CHILDREN_IDS_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					DOMNodeTraitDefinition.CHILDREN_PROPERTY, DOMNodeTraitDefinition.CHILDREN_IDS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
