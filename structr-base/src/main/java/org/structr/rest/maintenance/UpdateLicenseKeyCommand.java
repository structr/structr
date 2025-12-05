/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.rest.maintenance;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.docs.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maintenance command to update the license key.
 *
 * At the moment, the license key is stored on the local disk. This has to be reviewed.
 *
 */
public class UpdateLicenseKeyCommand extends Command implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(UpdateLicenseKeyCommand.class.getName());

	@Override
	public Class getServiceClass() {
		return null;
	}

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		// The new license key is of course mandatory
		final String newLicenseKey = (String) attributes.get("newLicenseKey");

		try {
			if (StringUtils.isNotBlank(newLicenseKey)) {
				writeNewLicenseKeyFile(newLicenseKey);
			}
		} catch (final IOException ioex) {
			final String errorMessage = "Unable to write license key file";
			logger.error(errorMessage, ioex.getMessage());
			throw new FrameworkException(422, errorMessage);
		}

		// If true, the system will immediately restart with the new key
		final Boolean restart = Boolean.TRUE.equals(attributes.get("restartImmediately"));

		if (restart) {
			logger.info("Refreshing license manager now...");
			Services.getInstance().getLicenseManager().refresh();
			logger.info("License manager refreshed.");
		}

	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	@Override
	public Map<String, String> getCustomHeaders() {
		return Collections.EMPTY_MAP;
	}

	private void writeNewLicenseKeyFile(final String newLicenseKey) throws IOException {

		final File licenseKeyFile = new File(Settings.getBasePath() + "license.key");
		if (licenseKeyFile.exists() || licenseKeyFile.createNewFile()) {

			try (final FileWriter fileWriter = new FileWriter(licenseKeyFile)) {

				fileWriter.write(newLicenseKey);
			}
		}
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.Hidden;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getShortDescription() {
		return "";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of();
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}
}
