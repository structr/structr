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
package org.structr.api.service;

/**
 * A callback that runs once the service layer has finished initializing.
 *
 * It normally runs exactly once per JVM. The exception is a callback that opts in via
 * {@link #rerunAfterDatabaseChange()}, which also runs again whenever the graph database underneath it
 * is replaced.
 */
public interface InitializationCallback {

	void initializationDone();

	default int priority() {

		return 0;
	}

	/**
	 * Whether this callback must run again after the graph database has been replaced.
	 *
	 * Structr starts on an in-memory database when there is no configuration file yet, so on a first
	 * start the callbacks run against a graph that is thrown away as soon as the real database is
	 * configured and NodeService restarts onto it. A callback that only starts a listener does not care.
	 * One that SEEDS DATA does: what it wrote is gone, and without opting in here it is never asked
	 * again, so the instance runs on without it until someone restarts the JVM.
	 *
	 * Opting in means giving up the exactly-once guarantee: the callback may run several times in one
	 * JVM and must be idempotent, creating only what is missing rather than assuming an empty graph.
	 * That is why this defaults to false - existing callbacks keep the old behaviour unless they are
	 * written for the new one.
	 *
	 * Note that overriding this is not a one-line change if the callback is registered as a lambda, as
	 * most are: a lambda implements the single abstract method and cannot override a default method, so
	 * the registration has to become an anonymous class implementing both.
	 */
	default boolean rerunAfterDatabaseChange() {

		return false;
	}
}
