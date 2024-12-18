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
package org.structr.web.entity.dom;

import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTrait;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.Map;

public interface DOMElement extends DOMNode, Element, NamedNodeMap {

	String GET_HTML_ATTRIBUTES_CALL = "return (Property[]) org.apache.commons.lang3.ArrayUtils.addAll(super.getHtmlAttributes(), _html_View.properties());";

	String lowercaseBodyName = "body";

	String EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT = "htmlEvent";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION = "structrIdExpression";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET = "structrTarget";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRDATATYPE = "structrDataType";
	String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD = "structrMethod";
	String EVENT_ACTION_MAPPING_PARAMETER_CHILDID = "childId";
	String EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT = "sourceObject";
	String EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY = "sourceProperty";
	int HtmlPrefixLength = PropertyView.Html.length();

	// ----- public methods -----
	String getTag();
	String getEventMapping();
	String getRenderingMode();
	String getDelayOrInterval();
	String getDataReloadTarget();

	boolean isManualReloadTarget();
	Iterable<DOMElement> getReloadSources();

	Iterable<PropertyKey> getHtmlAttributes();
	Iterable<String> getHtmlAttributeNames();

	void openingTag(final AsyncBuffer out, final String tag, final RenderContext.EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException;

	boolean isInsertable();
	boolean isFromWidget();

	Iterable<ActionMapping> getTriggeredActions();
	Iterable<ParameterMapping> getParameterMappings();

	Map<String, Object> getMappedEvents();

	String getOffsetAttributeName(final String name, final int offset);

	static int intOrOne(final String source) {

		if (source != null) {

			try {

				return Integer.valueOf(source);

			} catch (Throwable t) {
			}
		}

		return 1;
	}

	static String toHtmlAttributeName(final String camelCaseName) {

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
}
