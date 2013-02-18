/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.log;

import org.fusesource.hawtdb.api.TxPageFile;
import org.fusesource.hawtdb.api.TxPageFileFactory;

import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.SingletonService;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * The log service main class.
 * 
 * @author Axel Morgner
 */
public class LogService implements SingletonService {

	private static final Logger logger                       = Logger.getLogger(LogService.class.getName());

	//~--- fields ---------------------------------------------------------

	private TxPageFileFactory logDbFactory	= null;
	private TxPageFile logDb		= null;

	/** Dependent services */
	private Set<RunnableService> registeredServices = new HashSet<RunnableService>();
	private boolean isInitialized                   = false;

	//~--- methods --------------------------------------------------------

	// <editor-fold defaultstate="collapsed" desc="interface SingletonService">
	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("logDb", logDb);
		}

	}

	@Override
	public void initialize(Map<String, String> context) {

//              String dbPath = (String) context.get(Services.DATABASE_PATH);
		String logDbPath = Services.getLogDatabasePath();

		try {

			logger.log(Level.INFO, "Initializing log database ({0}) ...", logDbPath);

			if (logDbFactory != null) {

				logger.log(Level.INFO, "Log database already running ({0}) ...", logDbPath);

				return;

			}

			try {

				logDbFactory = new TxPageFileFactory();

				logDbFactory.setFile(new File(logDbPath));
				logDbFactory.open();

				logDb = logDbFactory.getTxPageFile();

			} catch (Throwable t) {

				logger.log(Level.INFO, "Log Database could not be started", logDbPath);

			}

			logger.log(Level.INFO, "Log database ready.");

		} catch (Exception e) {

			logger.log(Level.SEVERE, "Log database could not be initialized. {0}", e.getMessage());
			e.printStackTrace(System.out);

		}

		isInitialized = true;
	}

	@Override
	public void shutdown() {

		if (isRunning()) {

			for (RunnableService s : registeredServices) {

				s.stopService();
			}

			// Wait for all registered services to end
			waitFor(registeredServices.isEmpty());

			try {

				logDbFactory.close();

			} catch (IOException ex) {

				logger.log(Level.SEVERE, "Log database was not closed properly", ex);

			}

			logDbFactory         = null;
			isInitialized = false;

		}

	}

	public void registerService(final RunnableService service) {

		registeredServices.add(service);

	}

	public void unregisterService(final RunnableService service) {

		registeredServices.remove(service);

	}

	private void waitFor(final boolean condition) {

		while (!condition) {

			try {

				Thread.sleep(10);

			} catch (Throwable t) {}

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getName() {

		return LogService.class.getSimpleName();

	}

	// </editor-fold>
	@Override
	public boolean isRunning() {

		return ((logDbFactory != null) && isInitialized);

	}

}
