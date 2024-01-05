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
package org.structr.net.repository;

import org.structr.net.data.time.PseudoTime;

import java.util.Map;

/**
 */
public interface RepositoryObject {

	String getUuid();
	String getType();
	String getDeviceId();
	String getUserId();

	PseudoTime getCreationTime();
	PseudoTime getLastModificationTime();

	void onCommit(final String transactionId);

	void setProperty(final PseudoTime timestamp, final String transactionId, final String key, final Object value);
	Object getProperty(final PseudoTime timestamp, final String transactionId, final String key);

	Map<String, Object> getProperties(final PseudoTime timestamp, final String transactionId);
	Map<String, Object> getProperties(final PseudoTime timestamp);

	void addListener(final ObjectListener listener);
	void removeListener(final ObjectListener listener);
}
