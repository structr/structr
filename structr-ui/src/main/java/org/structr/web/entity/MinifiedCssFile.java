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
package org.structr.web.entity;

import com.yahoo.platform.yui.compressor.CssCompressor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.web.common.FileHelper;

public class MinifiedCssFile extends AbstractMinifiedFile {

	private static final Logger logger = Logger.getLogger(MinifiedCssFile.class.getName());

	public static final Property<Integer> lineBreak = new IntProperty("lineBreak").defaultValue(-1);

	public static final View defaultView = new View(MinifiedJavaScriptFile.class, PropertyView.Public, minificationSources, lineBreak);
	public static final View uiView      = new View(MinifiedJavaScriptFile.class, PropertyView.Ui, minificationSources, lineBreak);

	@Override
	public boolean shouldModificationTriggerMinifcation(ModificationEvent modState) {

		return modState.getModifiedProperties().containsKey(MinifiedCssFile.lineBreak);

	}

	@Override
	public void minify() throws FrameworkException, IOException {

		logger.log(Level.INFO, "Running minify: {0}", this.getType());

		FileHelper.setFileData(this, getConcatenatedSource().getBytes(), null);

		try (FileReader in = new FileReader(this.getFileOnDisk())) {
			final java.io.File temp = java.io.File.createTempFile("structr-minify", ".tmp");

			try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(temp))) {
				final CssCompressor compressor = new CssCompressor(in);
				compressor.compress(out, getProperty(lineBreak));
			}

			FileHelper.setFileData(this, FileUtils.readFileToString(temp).getBytes(), null);
		}
	}
}
