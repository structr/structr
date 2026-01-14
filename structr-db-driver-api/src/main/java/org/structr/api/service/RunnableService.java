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
 * A service that can be run in a separate thread. It is a good practice to
 * let your RunnableService implementation extend java.lang.Thread and map
 * the startService() and stopService() methods appropriately.
 *
 *
 */
public interface RunnableService extends Service {

	public void startService() throws Exception;

	public void stopService();

	/**
	 * Return true if the service should be started automatically
	 * on container startup
	 *
	 * @return runOnStartup
	 */
	public boolean runOnStartup();

	@Override
	public boolean isRunning();
	
}
