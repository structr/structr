/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.files;

import org.structr.api.config.Settings;
import org.structr.core.Services;
import org.structr.files.ftp.FtpService;
import org.structr.files.ssh.SSHService;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Base class for all structr UI tests.
 * All tests are executed in superuser context.
 *
 */
public abstract class StructrFileTestBase extends StructrUiTest {

	/* 0 lets the operating system pick a free port, and setup() reads back the one it picked, like
	   StructrUiTest does for HTTP. A port chosen at random here collided now and then between test
	   classes running in parallel, and the class that lost started without an FTP server. */
	protected int ftpPort = 0;
	protected int sshPort = 0;

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		Settings.Services.setValue("NodeService SchemaService FtpService SSHService");

		Settings.FtpPort.setValue(ftpPort);
		Settings.SshPort.setValue(sshPort);

		super.setup(testDatabaseConnection);

		final Services services = Services.getInstance();
		final FtpService ftpService = services.getServiceImplementation(FtpService.class);

		if (ftpService != null) {

			ftpPort = ftpService.getAllocatedPort();
		}

		final SSHService sshService = services.getServiceImplementation(SSHService.class);
		if (sshService != null) {

			sshPort = sshService.getAllocatedPort();
		}
	}
}

