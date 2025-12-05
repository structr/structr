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
package org.structr.web.traits.wrappers.dom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.api.Predicate;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.NonIndexed;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.operations.GetAttributes;
import org.structr.web.traits.operations.OpeningTag;

import java.util.List;
import java.util.Map;

public class DOMElementTraitWrapper extends DOMNodeTraitWrapper implements DOMElement, NonIndexed {

	private static final Gson gson = new GsonBuilder().create();

	public DOMElementTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	// ----- public methods -----
	@Override
	public String getTag() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.TAG_PROPERTY));
	}

	@Override
	public String getHtmlId() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition._HTML_ID_PROPERTY));
	}

	@Override
	public String getHtmlName() {
		// FIXME ? _html_name is only defined on some elements, wont this break?
		return wrappedObject.getProperty(traits.key("_html_name"));
	}

	@Override
	public String getEventMapping() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.EVENT_MAPPING_PROPERTY));
	}

	@Override
	public String getRenderingMode() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.DATA_STRUCTR_RENDERING_MODE_PROPERTY));
	}

	@Override
	public String getDelayOrInterval() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.DATA_STRUCTR_DELAY_OR_INTERVAL_PROPERTY));
	}

	@Override
	public String getDataReloadTarget() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.DATA_STRUCTR_RELOAD_TARGET_PROPERTY));
	}

	@Override
	public void setAttribute(final String name, final String value) throws FrameworkException {

		// FIXME: no clue how this will ever be called (outside from tests)
		final PropertyKey<String> key = findOrCreateAttributeKey(name);

		wrappedObject.setProperty(key, value);
	}

	@Override
	public String getNodeValue() {
		return null;
	}

	public Iterable<PropertyKey> getHtmlAttributes() {
		return traits.getMethod(GetAttributes.class).getHtmlAttributes(this);
	}

	public List<String> getHtmlAttributeNames() {
		return traits.getMethod(GetAttributes.class).getHtmlAttributeNames(this);
	}

	@Override
	public void openingTag(AsyncBuffer out, String tag, RenderContext.EditMode editMode, RenderContext renderContext, int depth) throws FrameworkException {
		traits.getMethod(OpeningTag.class).openingTag(this, out, tag, editMode, renderContext, depth);
	}

	@Override
	public boolean isInsertable() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.DATA_STRUCTR_INSERT_PROPERTY));
	}

	@Override
	public boolean isFromWidget() {
		return wrappedObject.getProperty(traits.key(DOMElementTraitDefinition.DATA_STRUCTR_FROM_WIDGET_PROPERTY));
	}

	@Override
	public Iterable<ActionMapping> getTriggeredActions() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMElementTraitDefinition.TRIGGERED_ACTIONS_PROPERTY);

		return Iterables.map(n -> n.as(ActionMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<ParameterMapping> getParameterMappings() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMElementTraitDefinition.PARAMETER_MAPPINGS_PROPERTY);

		return Iterables.map(n -> n.as(ParameterMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public Map<String, Object> getMappedEvents() {

		final String mapping = getEventMapping();
		if (mapping != null) {

			return gson.fromJson(mapping, Map.class);
		}

		return null;
	}

	@Override
	public boolean isTargetElement() {

		final String key = "cachedIsTargetElement";

		Boolean cachedIsTargetElement = (Boolean) wrappedObject.getNode().getCache().get(key);
		if (cachedIsTargetElement == null) {

			final boolean isManualReloadTarget = isManualReloadTarget();
			final List<DOMElement> reloadSources = Iterables.toList(getReloadSources());
			final List<ActionMapping> reloadingActions = Iterables.toList(getReloadingActions());
			final List<ActionMapping> failureActions = Iterables.toList(getFailureActions());
			final List<ActionMapping> successNotificationActions = Iterables.toList(getSuccessNotificationActions());
			final List<ActionMapping> failureNotificationActions = Iterables.toList(getFailureNotificationActions());

			cachedIsTargetElement = (isManualReloadTarget || !reloadSources.isEmpty() || !reloadingActions.isEmpty() || !failureActions.isEmpty() || !successNotificationActions.isEmpty() || !failureNotificationActions.isEmpty());

			wrappedObject.getNode().getCache().put(key, cachedIsTargetElement);
		}

		return cachedIsTargetElement;
	}

	// ----- private static methods -----
	private static int intOrOne(final String source) {

		if (source != null) {

			try {

				return Integer.valueOf(source);

			} catch (Throwable t) {
			}
		}

		return 1;
	}

	private static String toHtmlAttributeName(final String camelCaseName) {

		final StringBuilder buf = new StringBuilder();

		camelCaseName.chars().forEach(c -> {

			if (Character.isUpperCase(c)) {

				buf.append("-");
				c = Character.toLowerCase(c);

			}

			buf.append(Character.toString(c));
		});

		return buf.toString();
	}

	@Override
	public boolean isManualReloadTarget() {
		return false;
	}

	@Override
	public Iterable<DOMElement> getReloadSources() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(DOMElementTraitDefinition.RELOAD_SOURCES_PROPERTY);

		return Iterables.map(n -> n.as(DOMElement.class), wrappedObject.getProperty(key));
	}

	public class TagPredicate implements Predicate<DOMNode> {

		private String tagName = null;

		public TagPredicate(String tagName) {
			this.tagName = tagName;
		}

		@Override
		public boolean accept(final DOMNode obj) {

			if (obj.is(StructrTraits.DOM_ELEMENT)) {

				DOMElement elem = obj.as(DOMElement.class);

				if (tagName.equals(elem.getTag())) {
					return true;
				}
			}

			return false;
		}
	}

	private HtmlProperty findOrCreateAttributeKey(final String name) {

		// try to find native html property defined in DOMElement or one of its subclasses
		if (traits.hasKey(name)) {

			final PropertyKey<String> key = traits.key(name);

			if (key instanceof HtmlProperty h) {

				return h;
			}
		}

		// create synthetic HtmlProperty
		final HtmlProperty htmlProperty = new HtmlProperty(name);

		htmlProperty.setDeclaringTrait(Traits.getTrait(StructrTraits.DOM_ELEMENT));

		return htmlProperty;
	}
}