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
package org.structr.core.entity.relationship;

import org.structr.core.entity.Favoritable;
import org.structr.core.entity.ManyToMany;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;

public class PrincipalFAVORITEFavoritable extends ManyToMany<PrincipalInterface, Favoritable> {

	@Override
	public Class<PrincipalInterface> getSourceType() {
		return PrincipalInterface.class;
	}

	@Override
	public Class<Favoritable> getTargetType() {
		return Favoritable.class;
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return null;
	}

	@Override
	public void setSourceProperty(PropertyKey source) {

	}

	@Override
	public void setTargetProperty(PropertyKey target) {

	}

	@Override
	public PropertyKey getSourceProperty() {
		return null;
	}

	@Override
	public PropertyKey getTargetProperty() {
		return null;
	}

	@Override
	public String name() {
		return "FAVORITE";
	}
}
