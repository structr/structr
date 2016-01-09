/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core;

import org.structr.common.StructrConf;

//~--- interfaces -------------------------------------------------------------

/**
 * The base class for services in structr.
 *
 *
 */
public interface Service {

	/**
	 * Called by Services#createCommand before the command is returned to
	 * the user. Use this method to inject service-specific resources into your command
	 * objects so you can access them later in the <code>execute()</code> method.
	 *
	 * @param command
	 */
	public void injectArguments(Command command);

	/**
	 * Called {@link Services} after the service is instantiated to initialize
	 * service-specific resources etc.
	 *
	 * @param services
	 * @param config
	 *
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void initialize(final Services services, final StructrConf config) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

	/**
	 * Called before the service is discarded. Note that this method will not be called
	 * for instances of {@link PrototypeService}.
	 */
	public void shutdown();

	/**
	 * Called by the service layer when the service was initialized successfully
	 */
	public void initialized();

	/**
	 * Return name of service
	 * @return name
	 */
	public String getName();

	/**
	 * Return true if Service is running.
	 * @return isRunning
	 */
	public boolean isRunning();

	/**
	 * Return true if Service is vital for the start of Structr. The failure
	 * of vital services will stop Structr from starting and display an
	 * appropriate error message.
	 * @return a boolean
	 */
	public boolean isVital();

}
