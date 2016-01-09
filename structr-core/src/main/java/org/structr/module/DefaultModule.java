/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.module;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.core.Module;

/**
 * The default implementation of a structr module.
 *
 *
 */
public class DefaultModule implements Module {
	
	private Set<String> rawClasses = new LinkedHashSet<>();
	private Set<String> properties = new LinkedHashSet<>();
	private Set<String> resources  = new LinkedHashSet<>();
	private Set<String> libraries  = new LinkedHashSet<>();
	private String modulePath      = null;

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
