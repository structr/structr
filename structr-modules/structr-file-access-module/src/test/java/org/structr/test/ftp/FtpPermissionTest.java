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
package org.structr.test.ftp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.test.web.files.FtpTest;
import org.structr.web.entity.File;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1586: the SSH and FTP paths resolved a node by READABILITY and then went straight to the
 * storage provider. Structr's permission check sits in setProperty, on node properties - it never sees
 * byte I/O - so being allowed to see a file was enough to replace its contents, and a JS asset of a
 * public site is exactly such a file.
 */
public class FtpPermissionTest extends FtpTest {

	private static final String FILE_NAME = "asset.js";
	private static final String ORIGINAL  = "console.log('original');";
	private static final String TAMPERED  = "console.log('tampered');";

	@Test
	public void testStoreDoesNotOverwriteAFileTheUserMayOnlyRead() {

		final String fileId = createReadableFileOwnedBySomeoneElse();
		final FTPClient attacker = setupFTPClient("ftpattacker");

		try (final Tx tx = app.tx()) {

			attacker.setFileType(FTP.ASCII_FILE_TYPE);

			final InputStream in = IOUtils.toInputStream(TAMPERED, StandardCharsets.UTF_8);

			attacker.storeFile(FILE_NAME, in);

			in.close();

			tx.success();

		} catch (IOException | FrameworkException ex) {

			fail("Unexpected exception: " + ex.getMessage());
		}

		disconnect(attacker);

		assertEquals("A file the caller may only read must keep its content", ORIGINAL, contentOf(fileId));
	}

	/**
	 * The deletion itself is refused centrally since ticket 1585 (DeleteNodeCommand checks
	 * Permission.delete), which the first assertion below confirms. The second is the part that belongs
	 * to this ticket: AbstractStructrFtpFile.delete() swallows the refusal and returns true regardless,
	 * so the client is told the file is gone while it is still there.
	 */
	@Test
	public void testDeleteOfAForeignFileIsRefusedAndReported() {

		final String fileId = createReadableFileOwnedBySomeoneElse();
		final FTPClient attacker = setupFTPClient("ftpattacker2");
		boolean deleteReportedSuccess = true;

		try (final Tx tx = app.tx()) {

			deleteReportedSuccess = attacker.deleteFile(FILE_NAME);

			tx.success();

		} catch (IOException | FrameworkException ex) {

			fail("Unexpected exception: " + ex.getMessage());
		}

		disconnect(attacker);

		assertNotNull("A file the caller may only read must survive a DELE", nodeById(fileId));
		assertFalse("A refused DELE must be reported as a failure, not as success", deleteReportedSuccess);
	}

	// ----- private methods -----
	private String createReadableFileOwnedBySomeoneElse() {

		final FTPClient owner = setupFTPClient("ftpowner");
		String fileId         = null;

		try (final Tx tx = app.tx()) {

			owner.setFileType(FTP.ASCII_FILE_TYPE);

			final InputStream in = IOUtils.toInputStream(ORIGINAL, StandardCharsets.UTF_8);

			owner.storeFile(FILE_NAME, in);

			in.close();

			tx.success();

		} catch (IOException | FrameworkException ex) {

			fail("Unexpected exception storing the file: " + ex.getMessage());
		}

		disconnect(owner);

		// readable for everyone who is logged in, and nothing beyond that
		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.nodeQuery(StructrTraits.FILE).name(FILE_NAME).getFirst();

			assertNotNull("The owner's file must exist", file);

			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			fileId = file.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return fileId;
	}

	private NodeInterface nodeById(final String id) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, id);

			tx.success();

			return node;

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

			return null;
		}
	}

	private String contentOf(final String id) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, id);

			assertNotNull("The file must still exist", node);

			final String content = IOUtils.toString(node.as(File.class).getInputStream(), StandardCharsets.UTF_8);

			tx.success();

			return content;

		} catch (IOException | FrameworkException ex) {

			fail("Unexpected exception: " + ex.getMessage());

			return null;
		}
	}
}
