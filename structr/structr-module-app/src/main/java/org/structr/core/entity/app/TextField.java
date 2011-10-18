/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.entity.app;

import org.apache.commons.lang.StringUtils;
<<<<<<< HEAD
import org.structr.common.RequestCycleListener;
import org.structr.common.RenderMode;
import org.structr.common.RequestHelper;
=======

import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.common.RequestCycleListener;
import org.structr.common.SessionValue;
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.NodeSource;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class TextField extends FormField implements InteractiveNode, RequestCycleListener {

	private static final Logger logger = Logger.getLogger(TextField.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(SubmitButton.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	protected SessionValue<Object> errorSessionValue = null;
	private String mappedName                        = null;
	protected SessionValue<Object> sessionValue      = null;

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ sourceSlotName; }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new ExternalTemplateRenderer(false));
	}

	// ----- interface RequestCycleListener -----
	@Override
	public void onRequestStart() {}

	@Override
	public void onRequestEnd() {
		getErrorMessageValue().set(null);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/textfield.png";
	}

	// ----- interface InteractiveNode -----
	@Override
<<<<<<< HEAD
	public Object getValue(HttpServletRequest request)
	{
		Object value = getValueFromSource(request);
		String name = getName();
		String ret = null;

		// only return value from last request if we were redirected before
		if(RequestHelper.isRedirected(request))
		{
			value = getLastValue().get(request);
=======
	public Object getValue() {

		HttpServletRequest request = CurrentRequest.getRequest();
		Object value               = getValueFromSource();
		String name                = getName();
		String ret                 = null;

		// only return value from last request if we were redirected before
		if (CurrentSession.isRedirected()) {
			value = getLastValue().get();
		} else {
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885

			// otherwise, clear value in session
			getLastValue().set(request, null);
		}

		if (request == null) {
			return value;
		}

		if (request != null) {

			ret = request.getParameter(name);

			if (ret != null) {

				// Parameter is there
				if (ret.length() == 0) {

					// Empty value
					return null;
				} else {

					// store value in session, in case we get a redirect afterwards
<<<<<<< HEAD
					getLastValue().set(request, ret);
=======
					getLastValue().set(ret);

>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
					return ret;
				}
			} else {

				// Parameter is not in request
				return value;
			}
		}

		return null;
	}

	@Override
<<<<<<< HEAD
	public String getStringValue(HttpServletRequest request)
	{
		Object value = getValue(request);
		return (value != null ? value.toString() : null);
=======
	public String getStringValue() {

		Object value = getValue();

		return ((value != null)
			? value.toString()
			: null);
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
	}

	@Override
	public Class getParameterType() {
		return (String.class);
	}

	@Override
	public String getMappedName() {

		if (StringUtils.isNotBlank(mappedName)) {
			return (mappedName);
		}

		return (getName());
	}

	@Override
<<<<<<< HEAD
	public void setErrorValue(HttpServletRequest request, Object errorValue)
	{
		getErrorMessageValue().set(request, errorValue);
	}

	@Override
	public Object getErrorValue(HttpServletRequest request)
	{
		return(getErrorMessageValue().get(request));
	}

	@Override
	public String getErrorMessage(HttpServletRequest request)
	{
		Object errorValue = getErrorValue(request);
		if(errorValue != null)
		{
			return(errorValue.toString());
		}

		return(null);
	}

	// ----- interface RequestCycleListener -----
	@Override
	public void onRequestStart(HttpServletRequest request)
	{
	}

	@Override
	public void onRequestEnd(HttpServletRequest request)
	{
		getErrorMessageValue().set(request, null);
=======
	public Object getErrorValue() {
		return (getErrorMessageValue().get());
	}

	@Override
	public String getErrorMessage() {

		Object errorValue = getErrorValue();

		if (errorValue != null) {
			return (errorValue.toString());
		}

		return (null);
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
	}

	// ----- private methods -----
	private SessionValue<Object> getErrorMessageValue() {

		if (errorSessionValue == null) {
			errorSessionValue = new SessionValue<Object>(createUniqueIdentifier("errorMessage"));
		}

		return (errorSessionValue);
	}

	private SessionValue<Object> getLastValue() {

		if (sessionValue == null) {
			sessionValue = new SessionValue<Object>(createUniqueIdentifier("lastValue"));
		}

		return (sessionValue);
	}

	/**
	 * Follows any incoming DATA relationship and tries to obtain a data value with
	 * the mapped name from the relationship.
	 *
	 * @return the value or null
	 */
<<<<<<< HEAD
	private Object getValueFromSource(HttpServletRequest request)
	{
=======
	private Object getValueFromSource() {

>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
		List<StructrRelationship> rels = getIncomingDataRelationships();
		String sourceName              = this.getName();
		Object ret                     = null;

		// follow INCOMING DATA relationships to found data source for this input field
		for (StructrRelationship rel : rels) {

			// first one wins
			AbstractNode startNode = rel.getStartNode();

			if (startNode instanceof NodeSource) {

				// source name mapping present? use input field name otherwise
				if (rel.getRelationship().hasProperty(Key.sourceSlotName.name())) {

					sourceName =
						(String) rel.getRelationship().getProperty(Key.sourceSlotName.name());
				}

<<<<<<< HEAD
				NodeSource source = (NodeSource)startNode;
				if(source != null)
				{
					AbstractNode loadedNode = source.loadNode(request);
					if(loadedNode != null)
					{
=======
				NodeSource source = (NodeSource) startNode;

				if (source != null) {

					AbstractNode loadedNode = source.loadNode();

					if (loadedNode != null) {
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
						ret = loadedNode.getProperty(sourceName);
					}
				}
			}

			// if a value is found, return it, otherwise try the next data source
			if (ret != null) {
				break;
			}
		}

		return (ret);
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setMappedName(String mappedName) {
		this.mappedName = mappedName;
	}

	@Override
	public void setErrorValue(Object errorValue) {
		getErrorMessageValue().set(errorValue);
	}
}
