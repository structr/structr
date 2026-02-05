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
package org.structr.core.traits;

import com.google.common.collect.Comparators;
import org.structr.docs.Documentable;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TraitsManager {

	private static final TraitsInstance rootInstance = new TraitsInstance("ROOT");
	private static volatile TraitsInstance currentInstance = rootInstance;

	public static TraitsInstance getCurrentInstance() {
		return currentInstance;
	}

	public static TraitsInstance getRootInstance() {
		return rootInstance;
	}

	public static TraitsInstance createCopyOfRootInstance() {
		return rootInstance.createCopy(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(System.currentTimeMillis()));
	}

	public static void replaceCurrentInstance(final TraitsInstance newInstance) {
		TraitsManager.currentInstance = newInstance;
	}

	public static void addAllSystemTypes(final List<Documentable> documentables) {

		final TraitsInstance rootInstance = TraitsManager.getRootInstance();
		for (final String traitName : rootInstance.getAllTypes(t -> t.isNodeType())) {

			final Traits traits = rootInstance.getTraits(traitName);
			if (!traits.isHidden()) {

				documentables.add(traits);
			}
		}
	}
}
