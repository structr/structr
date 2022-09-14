/*
 * Copyright (C) 2010-2022 Structr GmbH
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
package org.structr.web.entity.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.renjin.repackaged.guava.base.CaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.EventContext;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.dom.DOMAttribute;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.DOMNodeList;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Input;
import org.structr.web.entity.html.Select;
import org.structr.web.function.InsertHtmlFunction;
import org.structr.web.function.RemoveDOMChildFunction;
import org.structr.web.function.ReplaceDOMChildFunction;
import org.structr.web.servlet.HtmlServlet;
import org.w3c.dom.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static org.structr.web.entity.dom.DOMNode.escapeForHtmlAttributes;

public interface ActionMapping extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema       = SchemaService.getDynamicSchema();
		final JsonObjectType type     = schema.addType("ActionMapping");
		final JsonObjectType node     = schema.addType("DOMNode");
		final JsonObjectType elem     = schema.addType("DOMElement");

		//type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/ActionMapping"));
		type.setExtends(URI.create("#/definitions/NodeInterface"));

		type.relate(node, "RELOAD_TARGET",   Cardinality.ManyToMany,"reloadingActions",   "reloadTargets");
		type.relate(node, "REDIRECT_TARGET", Cardinality.ManyToMany,"redirectingActions", "redirectTarget");
		type.relate(elem, "INPUTS",          Cardinality.ManyToMany,"processingActions",  "inputs");

		type.addStringProperty("event",        PropertyView.Ui).setHint("DOM event which triggers the action.");
		type.addStringProperty("action",       PropertyView.Ui).setHint("Action which will be triggered.");
		type.addStringProperty("method",       PropertyView.Ui).setHint("Name of method to execute when triggered action is 'method'");
		type.addStringProperty("dataType",     PropertyView.Ui).setHint("Data type for create action");
		type.addStringProperty("idExpression", PropertyView.Ui).setHint("Script expression that evaluates to the id of the object the method should be executed on.");
		type.addStringProperty("reloadMode",   PropertyView.Ui).setHint("Mode of reload after successful execution of action.");
		type.addStringProperty("reloadTarget", PropertyView.Ui).setHint("Traget of reload after successful execution of action.");

		type.addViewProperty(PropertyView.Ui, "triggerElement");
		type.addViewProperty(PropertyView.Ui, "reloadTargets");
		type.addViewProperty(PropertyView.Ui, "redirectTarget");
		type.addViewProperty(PropertyView.Ui, "inputs");

	}}


}
