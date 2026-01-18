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
package org.structr.module;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A Structr module record, contains all the information needed to
 * scan and initialize a module.
 */
public class ClasspathResource {

	private final Set<String> rawClasses = new LinkedHashSet<>();
	private final Set<String> properties = new LinkedHashSet<>();
	private final Set<String> resources  = new LinkedHashSet<>();
	private final Set<String> libraries  = new LinkedHashSet<>();
	private String modulePath            = null;

	public ClasspathResource(final String modulePath) {
		this.modulePath = modulePath;
	}

	public String getModulePath() {
		return modulePath ;
	}

	public Set<String> getClasses() {
		return rawClasses;
	}

	public Set<String> getProperties() {
		return properties;
	}

	public Set<String> getResources() {
		return resources;
	}

	public Set<String> getLibraries() {
		return libraries;
	}
}
