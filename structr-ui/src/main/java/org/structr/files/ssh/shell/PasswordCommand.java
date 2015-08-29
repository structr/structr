package org.structr.files.ssh.shell;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.StructrShellCommand;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class PasswordCommand extends InteractiveShellCommand {

	private String firstPassword  = null;
	private String secondPassword = null;


	@Override
	public void execute(final StructrShellCommand parent) throws IOException {

		term.setEcho(false);
		super.execute(parent);

		try (final Tx tx = StructrApp.getInstance(SecurityContext.getInstance(user, AccessMode.Backend)).tx()) {

			term.println("Changing password for " + user.getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Override
	public void displayPrompt() throws IOException {

		if (firstPassword == null) {

			try (final Tx tx = StructrApp.getInstance(SecurityContext.getInstance(user, AccessMode.Backend)).tx()) {

				term.print("Enter new password: ");

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
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

				try (final Tx tx = StructrApp.getInstance(SecurityContext.getInstance(user, AccessMode.Backend)).tx()) {

					user.setProperty(User.password, firstPassword);

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
