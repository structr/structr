/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
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
	public RelationshipType getRelationshipType(final String name) {
		return getOrCreateRelationshipType(name);
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
		return relTypeCache.computeIfAbsent(name, RelationshipType::forName);
	}
}
