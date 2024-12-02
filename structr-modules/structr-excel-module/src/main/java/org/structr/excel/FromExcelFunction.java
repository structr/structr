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
package org.structr.excel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.structr.api.util.Iterables;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.property.DateProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.schema.parser.DatePropertyParser;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FromExcelFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_FROM_EXCEL    = "Usage: ${from_excel(file[, encoding = \"UTF-8\"])}. Example: ${get_content(first(find('File', 'name', 'test.xlsx')))}";
	public static final String ERROR_MESSAGE_FROM_EXCEL_JS = "Usage: ${{Structr.fromExcel(file[, encoding = \"UTF-8\"])}}. Example: ${{Structr.getContent(fileNode)}}";

	@Override
	public String getName() {
		return "from_excel";
	}

	@Override
	public String getSignature() {
		return "file [, encoding=UTF-8 ]";
	}

	@Override
	public String getRequiredModule() {
		return "excel";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 1);

			if (sources[0] instanceof File) {

				final File file = (File)sources[0];

				if (StorageProviderFactory.getStorageProvider(file).size() == 0) {
					return "";
				}

				final int sheetIndex = (sources.length == 2 && sources[1] != null) ? Integer.parseInt(sources[1].toString()) : 0;

				try (final InputStream is = file.getInputStream()) {

					return readExcel(is, sheetIndex);

				} catch (FrameworkException fe) {

					throw fe;

				} catch (Throwable t) {

					throw new FrameworkException(400, "Error while reading excel file", t);
				}
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_FROM_EXCEL_JS : ERROR_MESSAGE_FROM_EXCEL);
	}

	@Override
	public String shortDescription() {
		return "Reads data from a given Excel sheet";
	}

	private Object readExcel(final InputStream is, final int sheetIndex) throws Throwable {

		final ConfigurationProvider provider = StructrApp.getConfiguration();


		final Gson gson = new GsonBuilder().create();
		List<Map<String, Object>> objects = new LinkedList<>();

		final Workbook workbook      = new XSSFWorkbook(is);
		final XSSFSheet sheet        = (XSSFSheet) workbook.getSheetAt(sheetIndex);

		final List<GraphObjectMap> elements = new LinkedList<>();

		final int firstRowNum = sheet.getFirstRowNum();
		final int lastRowNum  = sheet.getLastRowNum();

		logger.debug("First row: {}, last row: {}", firstRowNum, lastRowNum);

		// First row = header
		final XSSFRow headerRow = sheet.getRow(firstRowNum);

		final ArrayList<String> headerValues = new ArrayList<>();

		for (int c = headerRow.getFirstCellNum(); c <= headerRow.getLastCellNum(); c++) {
			final XSSFCell cell = headerRow.getCell(c);

			if (cell == null) {
				logger.warn("Skipping empty header cell (column index: {})", c);
				continue;
			}

			headerValues.add(getCellValue(cell).toString());
		}

		for (int r = firstRowNum+1; r <= lastRowNum; r++) {

			final XSSFRow row = sheet.getRow(r);

			if (row == null) {
				logger.debug("Row is null, skipping...");
				continue;
			}

			final GraphObjectMap rowObject = new GraphObjectMap();

			final Short firstCellNum = row.getFirstCellNum();
			final Short lastCellNum  = row.getLastCellNum();

			logger.debug("First cell: {}, last cell: {}", firstCellNum, lastCellNum);

			for (int c = firstCellNum; c < lastCellNum; c++) {

				final XSSFCell cell = row.getCell(c);

				if (cell != null) {

					if (headerValues.size() > c) {

						rowObject.put(new GenericProperty(headerValues.get(c)), getCellValue(cell));

					} else {
						logger.warn("No header value found for cell {}, skipping...", cell);
					}
				} else {
					logger.debug("Cell is null, skipping...");
				}
			}

			elements.add(rowObject);
		}

		return elements;
	}

	private Object getCellValue(final XSSFCell cell) {

		final CellType cellType = cell.getCellType();

		Object value = "";

		switch (cellType) {
			case _NONE:
				break;
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					value = cell.getDateCellValue();
				} else {
					value = cell.getNumericCellValue();
				}
				break;
			case STRING:
				value = cell.getStringCellValue();
				break;
			case FORMULA:
				value = cell.getCellFormula();
				break;
			case BLANK:
				value = cell.getStringCellValue();
				break;
			case BOOLEAN:
				value = cell.getBooleanCellValue();
				break;
			case ERROR:
				value = cell.getErrorCellValue();
				break;
		}

		return value;
	}

}

