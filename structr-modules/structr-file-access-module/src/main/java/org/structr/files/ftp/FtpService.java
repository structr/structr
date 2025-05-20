/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.*;
import org.structr.schema.SchemaService;

/**
 *
 *
 */
@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
@StartServiceInMaintenanceMode
public class FtpService implements RunnableService {

	private static final Logger logger = LoggerFactory.getLogger(FtpService.class.getName());
	private boolean isRunning          = false;

	private static int port;
	private static String passivePortRange;
	private FtpServer server;

	@Override
	public void startService() throws Exception {

		FtpServerFactory serverFactory = new FtpServerFactory();

		serverFactory.setUserManager(new StructrUserManager());
		serverFactory.setFileSystem( new StructrFileSystemFactory());

		ListenerFactory factory = new ListenerFactory();

		if (!StringUtils.isAllBlank(passivePortRange)) {
			DataConnectionConfigurationFactory dataConnConfigFactory = new DataConnectionConfigurationFactory();
			dataConnConfigFactory.setPassivePorts(passivePortRange);
			factory.setDataConnectionConfiguration(dataConnConfigFactory.createDataConnectionConfiguration());
		}

		factory.setPort(port);

		serverFactory.addListener("default", factory.createListener());

		logger.info("Starting FTP server on port {}", new Object[] { String.valueOf(port) });

		server = serverFactory.createServer();
		server.start();

		this.isRunning = true;

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
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		port = Settings.getSettingOrMaintenanceSetting(Settings.FtpPort).getValue();
		passivePortRange = Settings.getSettingOrMaintenanceSetting(Settings.FTPPassivePortRange).getValue();

		return new ServiceResult(true);
	}

	@Override
	public void initialized() {
	}

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

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "file-access";
	}
}
