/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.files.ftp;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.structr.common.StructrConf;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;

/**
 *
 *
 */
public class FtpService implements RunnableService {

	private static final Logger logger = Logger.getLogger(FtpService.class.getName());
	private boolean isRunning          = false;

	private static int port;
	private FtpServer server;

	public static final String APPLICATION_FTP_PORT = "application.ftp.port";

	@Override
	public void startService() {

		try {

			FtpServerFactory serverFactory = new FtpServerFactory();

			serverFactory.setUserManager(new StructrUserManager());
			serverFactory.setFileSystem( new StructrFileSystemFactory());

			ListenerFactory factory = new ListenerFactory();
			factory.setPort(port);
			serverFactory.addListener("default", factory.createListener());

			logger.log(Level.INFO, "Starting FTP server on port {0}", new Object[] { String.valueOf(port) });

			server = serverFactory.createServer();
			server.start();

			this.isRunning = true;

		} catch (FtpException ex) {

			logger.log(Level.SEVERE, null, ex);
		}

	}

	@Override
	public void stopService() {

		if (isRunning) {
			this.shutdown();
		}
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return !server.isStopped();
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(final Services services, final StructrConf config) {

		final StructrConf finalConfig = new StructrConf();

		// Default config
		finalConfig.setProperty(APPLICATION_FTP_PORT, "8021");

		Services.mergeConfiguration(finalConfig, config);

		port = Services.parseInt(finalConfig.getProperty(APPLICATION_FTP_PORT), 8021);
	}

	@Override
	public void initialized() {}

	@Override
	public void shutdown() {
		if (!server.isStopped()) {
			server.stop();
			this.isRunning = false;
		}
	}

	@Override
	public String getName() {
		return FtpServer.class.getSimpleName();
	}

	@Override
	public boolean isVital() {
		return false;
	}

}
