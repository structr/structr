/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core;

import org.structr.common.StructrConf;

//~--- interfaces -------------------------------------------------------------

/**
 * The base class for services in structr.
 *
 * @author cmorgner
 */
public interface Service {

	/**
	 * Called by {@link Services#createCommand()} before the command is returned to
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
	 * @param config
	 */
	public void initialize(final StructrConf config);

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
	 * @return
	 */
	public String getName();

	/**
	 * Return true if Service is running.
	 * @return
	 */
	public boolean isRunning();

}
