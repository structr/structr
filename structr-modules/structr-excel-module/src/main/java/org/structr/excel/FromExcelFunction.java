/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.GenericProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FromExcelFunction extends Function<Object, Object> {

	@Override
	public String getName() {
		return "fromExcel";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("file [, sheetIndexOrName = 0 ]");
	}

	@Override
	public String getRequiredModule() {
		return "excel";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			if (sources[0] instanceof NodeInterface n && n.is(StructrTraits.FILE)) {

				final File file = n.as(File.class);

				if (StorageProviderFactory.getStorageProvider(file).size() == 0) {
					return "";
				}

				try (final InputStream is = file.getInputStream()) {

					if (sources.length < 2 || sources[1] instanceof Integer) {

						final int sheetIndex = (sources.length == 2) ? (Integer)sources[1] : 0;

						return readExcel(is, sheetIndex);

					} else  {

						return readExcel(is, sources[1].toString());
					}

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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${fromExcel(file[, sheetIndexOrName = 0 ])}. Example: ${fromExcel(first(find('File', 'name', 'test.xlsx')))}"),
			Usage.javaScript("Usage: ${{ $.fromExcel(file[, sheetIndexOrName = 0 ]); }}. Example: ${{ $.fromExcel(fileNode); }}")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The columns in the first row of the excel sheet are used as headers and must be populated for columns to be read."
		);
	}

	@Override
	public String getShortDescription() {
		return "Reads data from a given Excel sheet.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("file", "excel file to read from"),
				Parameter.optional("sheetIndexOrName", "sheet index or name of sheet to read from (defaults to first sheet)")
		);
	}

	@Override
	public String getLongDescription() {
		return "The sheet can be passed as zero-indexed sheet number or by sheet name.";
	}

	private Object readExcel(final InputStream is, final String sheetName) throws Throwable {

		final Workbook workbook      = new XSSFWorkbook(is);
		final XSSFSheet sheet        = (XSSFSheet) workbook.getSheet(sheetName);

		if (sheet == null) {
			throw new FrameworkException(400, "Error while reading excel file: Sheet '" + sheetName + "' not found");
		}

		return readExcelSheet(sheet);
	}

	private Object readExcel(final InputStream is, final int sheetIndex) throws Throwable {

		final Workbook workbook      = new XSSFWorkbook(is);
		final int availableSheets    = workbook.getNumberOfSheets();

		if (sheetIndex < 0 || sheetIndex > (availableSheets - 1)) {

			throw new FrameworkException(400, "Error while reading excel file: Sheet index for given file can only be 0.." + (availableSheets - 1));
		}

		final XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(sheetIndex);

		return readExcelSheet(sheet);
	}

	private Object readExcelSheet(final XSSFSheet sheet) {

		final List<GraphObjectMap> elements = new LinkedList<>();

		final int firstRowNum = sheet.getFirstRowNum();
		final int lastRowNum  = sheet.getLastRowNum();

		logger.debug("First row: {}, last row: {}", firstRowNum, lastRowNum);

		// First row = header
		final XSSFRow headerRow = sheet.getRow(firstRowNum);

		final ArrayList<String> headerValues = new ArrayList<>();

		for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {

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

