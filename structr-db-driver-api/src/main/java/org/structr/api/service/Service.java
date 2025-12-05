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
package org.structr.api.service;

import org.structr.api.config.Settings;

/**
 * The base class for services in structr.
 */
public interface Service extends Feature {

	/**
	 * Called by Services#createCommand before the command is returned to
	 * the user. Use this method to inject service-specific resources into your command
	 * objects so you can access them later in the <code>execute()</code> method.
	 *
	 * @param command
	 */
	void injectArguments(Command command);

	/**
	 * Called by the service layer after the service is instantiated to initialize
	 * service-specific resources etc.
	 *
	 * @param services
	 * @param serviceName
	 *
	 * @throws ReflectiveOperationException
	 *
	 * @return service result object
	 */
	ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException;

	/**
	 * Called before the service is discarded. Note that this method will not be called
	 * for instances of {@link PrototypeService}.
	 */
	void shutdown();

	/**
	 * Called by the service layer when the service was initialized successfully
	 */
	void initialized();

	/**
	 * Return name of service
	 * @return name
	 */
	String getName();

	/**
	 * Return true if Service is running.
	 * @return isRunning
	 */
	boolean isRunning();

	/**
	 * Return true if Service is vital for the start of Structr. The failure
	 * of vital services will stop Structr from starting and display an
	 * appropriate error message.
	 * @return a boolean indicating whether the service is vital for startup
	 */
	boolean isVital();

	/**
	 * @return a boolean indicating whether the system should wait for this service to start and retry
	 */
	boolean waitAndRetry();

	default int getRetryCount() {
		return Settings.ServicesStartRetries.getValue(10);
	}

	default int getRetryDelay() {
		return Settings.ServicesStartTimeout.getValue(30);
	}
}
