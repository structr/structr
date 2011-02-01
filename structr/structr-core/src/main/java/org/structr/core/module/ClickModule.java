/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.module;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.core.Module;

/**
 *
 * @author Christian Morgner
 */
public class ClickModule implements Module
{
	private Set<String> rawClasses = new LinkedHashSet<String>();
	private Set<String> properties = new LinkedHashSet<String>();
	private Set<String> resources = new LinkedHashSet<String>();
	private String modulePath = null;

	public ClickModule(String modulePath)
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
}
