/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.pdf.function;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.core.storage.StorageProviderFactory;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.io.IOException;

public class PDFEncryptFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_PDF_ENCRYPT = "Usage: ${pdf_encrypt(file, password)}. Example: ${pdf_encrypt(first(find('File', 'name', 'document.pdf')), 'mypassword')}";

	private static final int keyLength = 256;

	@Override
	public String getName() {
		return "pdf_encrypt";
	}

	@Override
	public String getSignature() {
		return "file, password";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			try {

				if (!(sources[0] instanceof File)) {

					logParameterError(caller, sources, "First parameter is not a file object.", ctx.isJavaScriptContext());
					return usage(ctx.isJavaScriptContext());
				}

				if (!(sources[1] instanceof String)) {

					logParameterError(caller, sources, "Second parameter is not a string.", ctx.isJavaScriptContext());
					return usage(ctx.isJavaScriptContext());
				}

				final File pdfFileObject  = (File) sources[0];
				final String userPassword = (String) sources[1];

				final PDDocument pdDocument = PDDocument.load(StorageProviderFactory.getStreamProvider(pdfFileObject).getInputStream());

				final AccessPermission accessPermission = new AccessPermission();
				accessPermission.setCanPrint(false);

				// Owner password (to open the file with all permissions) is the superuser password
				final StandardProtectionPolicy standardProtectionPolicy = new StandardProtectionPolicy(Settings.SuperUserPassword.getValue(), userPassword, accessPermission);
				standardProtectionPolicy.setEncryptionKeyLength(keyLength);
				standardProtectionPolicy.setPermissions(accessPermission);
				pdDocument.protect(standardProtectionPolicy);

				if (StorageProviderFactory.getStreamProvider(pdfFileObject).getInputStream().available() <= 0) {
					pdDocument.save(StorageProviderFactory.getStreamProvider(pdfFileObject).getOutputStream());
				}

				pdDocument.close();

			} catch (final IOException ioex) {

				logException(caller, ioex, sources);
			}

		} catch (final ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (final ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_PDF_ENCRYPT;
	}

	@Override
	public String shortDescription() {
		return "Encrypts a PDF file so that it can't be opened without password.";
	}
}
