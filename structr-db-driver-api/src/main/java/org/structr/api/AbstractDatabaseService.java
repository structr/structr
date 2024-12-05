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
package org.structr.api;

import org.apache.commons.lang.StringUtils;
import org.structr.api.config.Settings;
import org.structr.api.graph.RelationshipType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public abstract class AbstractDatabaseService implements DatabaseService {

	private static final Map<String, RelationshipType> relTypeCache   = new ConcurrentHashMap<>();
	private static final long nanoEpoch                               = System.nanoTime();

	@Override
	public <T> T forName(final Class<T> type, final String name) {

		if (RelationshipType.class.equals(type)) {

			return (T)getOrCreateRelationshipType(name);
		}

		throw new RuntimeException("Cannot create object of type " + type);
	}

	@Override
	public String getTenantIdentifier() {

		final String tenantId = Settings.TenantIdentifier.getValue();

		if (StringUtils.isBlank(tenantId)) {

			return null;
		}

		return tenantId;
	}

	@Override
	public String getInternalTimestamp(final long millisOffset, final long nanosOffset) {

		final String millis = StringUtils.leftPad(Long.toString(System.currentTimeMillis() + millisOffset), 18, "0");
		final String nanos  = StringUtils.leftPad(Long.toString(System.nanoTime() - nanoEpoch + nanosOffset), 18, "0");

		return millis + "." + nanos;
	}

	// ----- private methods -----
	private RelationshipType getOrCreateRelationshipType(final String name) {

		RelationshipType relType = relTypeCache.get(name);
		if (relType == null) {

			relType = new RelationshipTypeImpl(name);
			relTypeCache.put(name, relType);
		}

		return relType;
	}

	// ----- nested classes -----
	private static class RelationshipTypeImpl implements RelationshipType {

		private String name = null;

		private RelationshipTypeImpl(final String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String getSourceType() {
			return null;
		}

		@Override
		public String getTargetType() {
			return null;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(final Object other) {

			if (other instanceof RelationshipType) {
				return other.hashCode() == hashCode();
			}

			return false;
		}
	}
}
