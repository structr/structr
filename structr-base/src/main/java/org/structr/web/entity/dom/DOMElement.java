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
import org.structr.core.property.PropertyKey;
import org.structr.web.servlet.HtmlServlet;
import org.w3c.dom.Element;

import java.util.Set;

public interface DOMElement extends DOMNode, Element {

	final String GET_HTML_ATTRIBUTES_CALL = "return (Property[]) org.apache.commons.lang3.ArrayUtils.addAll(super.getHtmlAttributes(), _html_View.properties());";
	Set<String> RequestParameterBlacklist = Set.of(HtmlServlet.ENCODED_RENDER_STATE_PARAMETER_NAME);

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

	boolean hasSharedComponent();

	Iterable<PropertyKey> getHtmlAttributes();

}
