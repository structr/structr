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
package org.structr.common.event;

import org.structr.api.Predicate;
import org.structr.core.entity.Principal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * An event log implementation that records runtime events and stores
 * them for a limited amount of time.
 */
public class RuntimeEventLog {

	public static final String METHOD_KEY = "method";
	public static final String PATH_KEY   = "path";
	public static final String ID_KEY     = "id";
	public static final String NAME_KEY   = "name";

	private static final BlockingDeque<RuntimeEvent> events = new LinkedBlockingDeque<>(101_000);

	private enum EventType {
		Transaction, Maintenance, Cron, ResourceAccess, Authentication, Rest, Http, Csv, Scripting, SystemInfo, GraphQL
	}

	public static void resourceAccess(final String description, final Map<String, Object> data) {
		add(EventType.ResourceAccess, description, data);
	}

	public static void failedLogin(final String description, final Map<String, Object> data) {
		add(EventType.Authentication, description, data);
	}

	public static void login(final String description, final Map<String, Object> data) {
		add(EventType.Authentication, description, data);
	}

	public static void token(final String description, final Map<String, Object> data) {
		add(EventType.Authentication, description, data);
	}

	public static void logout(final String description, final Map<String, Object> data) {
		add(EventType.Authentication, description, data);
	}

	public static void registration(final String description, final Map<String, Object> data) {
		add(EventType.Authentication, description, data);
	}

	public static void transaction(final String status) {
		add(EventType.Transaction, status);
	}

	public static void transaction(final String status, final Map<String, Object> data) {
		add(EventType.Transaction, status, data);
	}

	public static void cron(final String command) {
		add(EventType.Cron, command);
	}

	public static void cron(final String command, final Map<String, Object> data) {
		add(EventType.Cron, command, data);
	}

	public static void maintenance(final String command) {
		add(EventType.Maintenance, command);
	}

	public static void maintenance(final String command, final Map<String, Object> data) {
		add(EventType.Maintenance, command, data);
	}

	public static void rest(final String method, final String path, final Principal user) {

		if (user != null) {

			add(EventType.Rest, method, Map.of(
					METHOD_KEY, method,
					PATH_KEY,   path,
					ID_KEY,     user.getUuid(),
					NAME_KEY,   user.getName()
			));

		} else {

			add(EventType.Rest, method, Map.of(
					METHOD_KEY, method,
					PATH_KEY,   path
			));
		}
	}

	public static void csv(final String method, final String path, final Principal user) {

		if (user != null) {

			add(EventType.Csv, method, Map.of(
					METHOD_KEY, method,
					PATH_KEY,   path,
					ID_KEY,     user.getUuid(),
					NAME_KEY,   user.getName()
			));

		} else {

			add(EventType.Csv, method, Map.of(
					METHOD_KEY, method,
					PATH_KEY,   path
			));
		}
	}

	public static void graphQL(final String query, final Principal user) {

		if (user != null) {

			add(EventType.GraphQL, query, Map.of(
					ID_KEY,   user.getUuid(),
					NAME_KEY, user.getName()
			));

		} else {

			add(EventType.Csv, query);
		}
	}

	public static void http(final String path, final Principal user) {

		if (user != null) {

			add(EventType.Http, path, Map.of(
					ID_KEY, user.getUuid(),
					NAME_KEY, user.getName()
			));

		} else {

			add(EventType.Http, path);
		}
	}

	public static void scripting(final String errorName, final Map<String, Object> data) {
		add(EventType.Scripting, errorName, data);
	}

	public static void systemInfo(final String info, final Map<String, Object> data) {
		add(EventType.SystemInfo, info, data);
	}

	public static List<RuntimeEvent> getEvents() {
		return getEvents(null);
	}

	public static List<RuntimeEvent> getEvents(final Predicate<RuntimeEvent> filter) {

		final List<RuntimeEvent> list = new LinkedList<>();

		for (final RuntimeEvent event : events) {

			if (filter == null || filter.accept(event)) {

				list.add(event);
			}
		}

		return list;
	}

	public static void acknowledgeAllEventsForId(final String uuid) {

		if (uuid != null) {

			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get(RuntimeEvent.ID_PROPERTY))).stream().forEach(e -> e.acknowledge());
		}
	}

	// ----- private methods -----
	private static void add(final EventType type, final String description) {
		add(type, description, Collections.EMPTY_MAP);
	}

	private static void add(final EventType type, final String description, final Map<String, Object> data) {

		events.addFirst(new RuntimeEvent(type.name(), description, data));

		if (events.remainingCapacity() < 1000) {
			events.removeLast();
		}
	}
}
