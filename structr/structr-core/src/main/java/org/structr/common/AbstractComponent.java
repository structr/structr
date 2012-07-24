/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

package org.structr.common;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractComponent
{
	private List<AbstractComponent> components = null;
	private Map<String, Set> attributes = null;
	private boolean forceClosingTag = false;
	private String id = null;

	public abstract void initComponents();

	public AbstractComponent()
	{
		this.attributes = new LinkedHashMap<String, Set>();
		this.components = new LinkedList<AbstractComponent>();
	}

	public AbstractComponent add(AbstractComponent content)
	{
		if(content != null)
		{
			this.getComponents().add(content);

			content.initComponents();
		}

		return(this);
	}

	public AbstractComponent addCssClass(Object cssClass)
	{
		Set cssClasses = getAttributes("class");
		cssClasses.add(cssClass);

		return(this);
	}

	public AbstractComponent addAttribute(String key, Object value)
	{
		Set values = getAttributes(key);
		values.add(value);

		return(this);
	}

	public AbstractComponent setId(String id)
	{
		this.id = id;

		return(this);
	}

	public String getId()
	{
		return(id);
	}

	public AbstractComponent setForceClosingTag(boolean forceClosingTag)
	{
		this.forceClosingTag = forceClosingTag;

		return(this);
	}

	public List<AbstractComponent> getComponents()
	{
		return(components);
	}

	public void doBeforeRendering()
	{
	}

	public Object[] getContent() {
		// override me
		return(null);
	}

	// <editor-fold defaultstate="collapsed" desc="private methods">
	private Set getAttributes(String key)
	{
		Set ret = getAttributes().get(key);

		if(ret == null)
		{
			ret = new LinkedHashSet();
			getAttributes().put(key, ret);
		}

		return(ret);
	}
	// </editor-fold>

	/**
	 * @return the attributes
	 */
	public Map<String, Set> getAttributes() {
		return attributes;
	}

	/**
	 * @return the forceClosingTag
	 */
	public boolean getForceClosingTag() {
		return forceClosingTag;
	}
}
