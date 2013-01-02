/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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
package org.structr.core.module;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.core.Module;

/**
 * The default implementation of a structr module.
 *
 * @author Christian Morgner
 */
public class DefaultModule implements Module
{
	private Set<String> rawClasses = new LinkedHashSet<String>();
	private Set<String> properties = new LinkedHashSet<String>();
	private Set<String> resources = new LinkedHashSet<String>();
	private Set<String> libraries = new LinkedHashSet<String>();
	private String modulePath = null;

	public DefaultModule(String modulePath)
	{
		this.modulePath = modulePath;
	}

	@Override
	public String getModulePath()
	{
		return(modulePath);
	}

	@Override
	public Set<String> getClasses()
	{
		return(rawClasses);
	}

	@Override
	public Set<String> getProperties()
	{
		return(properties);
	}

        @Override
	public Set<String> getResources()
	{
		return(resources);
	}

        @Override
	public Set<String> getLibraries()
	{
		return(libraries);
	}
}
