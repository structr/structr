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
package org.structr.files.ssh.shell;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.web.entity.User;

import java.io.IOException;

/**
 *
 *
 */
public class PasswordCommand extends InteractiveShellCommand {

	private static final Logger logger = LoggerFactory.getLogger(PasswordCommand.class.getName());

	private String firstPassword  = null;
	private String secondPassword = null;

	@Override
	public void execute(final StructrShellCommand parent) throws IOException {

		term.setEcho(false);
		super.execute(parent);

		try (final Tx tx = StructrApp.getInstance().tx()) {

			term.println("Changing password for " + user.getName());

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	@Override
	public void displayPrompt() throws IOException {

		if (firstPassword == null) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				term.print("Enter new password: ");

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}

		} else if (secondPassword == null) {

			term.print("Confim password: ");
		}
	}

	@Override
	public void handleLine(final String line) throws IOException {

		if (firstPassword == null) {

			if (StringUtils.isEmpty(line)) {

				term.println("Empty password not permitted.");

			} else {

				firstPassword = line;
			}

		} else if (secondPassword == null) {

			secondPassword = line;

			if (firstPassword.equals(secondPassword)) {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					user.setProperty(StructrApp.key(User.class, "password"), firstPassword);

					tx.success();

					term.println("Password changed.");

				} catch (FrameworkException fex) {
					term.println("Password NOT changed: " + fex.getMessage());
				}

			} else {

				term.println("Password NOT changed, passwords don't match.");
			}

			term.restoreRootTerminalHandler();
		}
	}
}
