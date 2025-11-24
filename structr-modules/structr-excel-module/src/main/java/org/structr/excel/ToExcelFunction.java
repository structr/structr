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

import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.property.DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.schema.parser.DatePropertyGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class ToExcelFunction extends Function<Object, Object> {

	@Override
	public String getName() {
		return "to_excel";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("nodes, propertiesOrView [, includeHeader, localizeHeader, headerLocalizationDomain, maxCellLength, overflowMode ]");
	}

	@Override
	public String getRequiredModule() {
		return "excel";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 7);

			if ( !(sources[0] instanceof Iterable) ) {
				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return "ERROR: First parameter must be a collection! ".concat(usage(ctx.isJavaScriptContext()));
			}

			final List<GraphObject> nodes           = Iterables.toList((Iterable)sources[0]);
			boolean includeHeader                   = true;
			boolean localizeHeader                  = false;
			String headerLocalizationDomain         = null;
			Integer maxCellLength                   = 32767;
			String overflowMode                     = "o";
			String propertyView                     = null;
			List<String> properties                 = null;

			switch (sources.length) {
				case 7: overflowMode             = sources[6].toString();
				case 6: maxCellLength            = Math.min(maxCellLength, (Integer)sources[5]);
				case 5: headerLocalizationDomain = sources[4].toString();
				case 4: localizeHeader           = (Boolean)sources[3];
				case 3: includeHeader            = (Boolean)sources[2];
				case 2: {

					if (sources[1] instanceof String) {

						propertyView = (String)sources[1];

					} else if (sources[1] instanceof List) {

						properties = (List)sources[1];

						if (properties.size() == 0) {

							logger.info("to_excel(): Unable to create Excel if list of properties is empty - returning empty Excel");
							return "";
						}

					} else {

						logParameterError(caller, sources, ctx.isJavaScriptContext());
						return "ERROR: Second parameter must be a collection of property names or a single property view!".concat(usage(ctx.isJavaScriptContext()));
					}
				}
			}

			// if we are using a propertyView, we need extract the property names from the first object which can not work without objects
			if (nodes.size() == 0 && propertyView != null) {

				logger.info("to_excel(): Unable to create Excel if no nodes are given - returning empty Excel");
				return "";
			}

			try {

				final Workbook workbook                  = writeExcel(nodes, propertyView, properties, includeHeader, localizeHeader, headerLocalizationDomain, ctx.getLocale(), maxCellLength, overflowMode);
				final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

				workbook.write(outputStream);

				return outputStream.toString("ISO-8859-1");

			} catch (Throwable t) {

				logger.warn("to_excel(): Exception occurred", t);
				return "";
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
				Usage.structrScript("Usage: ${to_excel(nodes, propertiesOrView[, includeHeader[, localizeHeader[, headerLocalizationDomain[, maxCellLength[, overflowMode]]]]])}. Example: ${to_excel(find('Page'), 'ui')}"),
				Usage.javaScript("Usage: ${{ $.toExcel(nodes, propertiesOrView[, includeHeader[, localizeHeader[, headerLocalizationDomain[, maxCellLength[, overflowMode]]]]]); }}. Example: ${{ $.toExcel($.find('Page'), 'ui')); }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("nodes", "Collection of objects (these objects can be database nodes or javascript objects)"),
				Parameter.mandatory("propertiesOrView", "Name of a view (e.g. ui or public) or a collection of property names (e.g. `merge('id', 'name')` in StructrScript or `['id', 'name']` in JavaScript). If the nodes parameter was a collection of javascript objects this needs to be a collection of property names. If the nodes parameter was a collection of database nodes, a collection of property names or a view name can be used."),
				Parameter.optional("includeHeader", "Switch indicating if a header row should be printed (default: `true`)"),
				Parameter.optional("localizeHeader", "Switch indicating if the column names in the header should be localized (default: `false`)"),
				Parameter.optional("headerLocalizationDomain", "lookup domain for localization of header names"),
				Parameter.optional("maxCellLength", "maximum length after which content cells are truncated"),
				Parameter.optional("overflowMode",
						"Controls how content that is longer than maxCellLength is handled (affects content-rows only - the header remains untouched)"
								+ "<p>`t`: Truncates the content at maxCellLength<br>"
								+ "`o` (default): Overflows the remaining text after maxCellLength into a cell comment. (This is restricted to 32767 bytes by Excel)</p>"
								+ "Any other value is used as is as a cell comment. This is useful to display a message like \"The content of this cell has been truncated\".")
		);
	}

	@Override
	public String getShortDescription() {
		return "Creates Excel from given data.";
	}

	@Override
	public String getLongDescription() {
		return "Returns XLSX string representation of the given collection of objects with the configured header view/columns.";
	}

	@Override
	public List<Example>getExamples() {
		return List.of(
				Example.structrScript("${set_content(create('File', 'name', 'new_document.xlsx'), to_excel(find('User'), 'public'), 'ISO-8859-1')}", "Excel file with view 'public' of user list."),
				Example.structrScript("""
				${
					(
						set_response_header('Content-Disposition', 'attachment; filename="user-export.xlsx"'),
						to_excel(find('User'), 'public')
					)
				}
				""", "Downloadable dynamic file containing the list of users in StructrScript. (See note regarding content-type)"),
				Example.javaScript("""
				${{
					$.setResponseHeader('Content-Disposition', 'attachment; filename="user-export.xlsx"');
					$.print($.toExcel($.find('User'), 'public'));
				}}
				""", "Downloadable dynamic file with view 'public' of user list of users in JavaScript. (See note regarding content-type)"),
				Example.javaScript("""
				${{
					$.setResponseHeader('Content-Disposition', 'attachment; filename="users-truncated.xlsx"');
					$.print($.toExcel($.find('User'), 'excelView', true, false, '', 1000, 't'));
				}}
				""", "Downloadable dynamic file with view 'excelView' of user list where headers are included but not localized and all cells are truncated after 1000 characters. (See note regarding content-type)")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The output of this function is a complete excel file, so the complete data must be contained in the `nodes` parameter and can not be appended later on",
			"This function is intended to be used in conjunction with the `set_content()` function or in a dynamic file",
			"**IMPORTANT**: The dynamic file should have the charset `ISO-8859-1` specified in its `contentType` (e.g. `application/octet-stream; charset=ISO-8859-1`)",
			"The content of the header row depends on the contents of `propertiesOrView` and the localization configuration.",
			"If a view is given, the (optionally localized) property names of that view are used as header row",
			"If a collection of properties is given, these (optionally localized) property names are used as a header row",
			"**IMPORTANT**: If you are creating Excel document from a dynamic file, make sure that there are no extraneous whitespaces after the dynamic script content. This is very hard to find and Excel will warn the user that the created file is corrupt and has to be repaired!",
			"""
				Limitations: Structr is creating `EXCEL2007` spreadsheets. The following limitations are taken directly from the [documentation](https://poi.apache.org/apidocs/org/apache/poi/ss/SpreadsheetVersion.html):
					- Maximum length of text cell contents is 32767
					- Maximum length of text cell comments is 32767
					- The total number of available rows is 1M (2^20)
					- The total number of available columns is 16K (2^14)
					- The maximum number of arguments to a function is 255
					- Number of conditional format conditions on a cell is unlimited (actually limited by available memory in Excel)
					- Number of cell styles is 64000
				"""
		);
	}

	public Workbook writeExcel(final List list, final String propertyView, final List<String> properties, final boolean includeHeader, final boolean localizeHeader, final String headerLocalizationDomain, final Locale locale, final Integer maxCellLength, final String overflowMode) throws IOException {

		final Workbook workbook      = new XSSFWorkbook();
		final CreationHelper factory = workbook.getCreationHelper();
		final XSSFSheet sheet        = (XSSFSheet) workbook.createSheet();
		final Drawing drawing        = sheet.createDrawingPatriarch();

		int rowCount  = 0;
		int cellCount = 0;

		XSSFRow currentRow = null;
		XSSFCell cell      = null;

		if (includeHeader) {

			currentRow = sheet.createRow(rowCount++);
			cellCount  = 0;

			if (propertyView != null) {

				final Object obj = list.get(0);

				if (obj instanceof GraphObject) {

					for (PropertyKey key : ((GraphObject)obj).getPropertyKeys(propertyView)) {

						cell = currentRow.createCell(cellCount++);

						String value = key.dbName();
						if (localizeHeader) {
							try {
								value = LocalizeFunction.getLocalization(locale, value, headerLocalizationDomain);
							} catch (FrameworkException fex) {
								logger.warn("to_excel(): Exception", fex);
							}
						}

						cell.setCellValue(value);
					}

				} else {

					cell = currentRow.createCell(cellCount++);
					cell.setCellValue("Error: Object is not of type GraphObject, can not determine properties of view for header row");
				}

			} else if (properties != null) {

				for (final String colName : properties) {

					cell = currentRow.createCell(cellCount++);
					String value = colName;
					if (localizeHeader) {
						try {
							value = LocalizeFunction.getLocalization(locale, value, headerLocalizationDomain);
						} catch (FrameworkException fex) {
							logger.warn("to_excel(): Exception", fex);
						}
					}

					cell.setCellValue(value);
				}
			}
		}

		for (final Object obj : list) {

			currentRow = sheet.createRow(rowCount++);
			cellCount = 0;

			if (propertyView != null) {

				if (obj instanceof GraphObject) {

					for (PropertyKey key : ((GraphObject)obj).getPropertyKeys(propertyView)) {

						final Object value = ((GraphObject)obj).getProperty(key);
						cell               = currentRow.createCell(cellCount++);

						writeToCell(factory, drawing, cell, value, maxCellLength, overflowMode);
					}

				} else {
					cell = currentRow.createCell(cellCount++);
					cell.setCellValue("Error: Object is not of type GraphObject, can not determine properties of object");
				}

			} else if (properties != null) {

				if (obj instanceof GraphObjectMap) {

					final Map convertedMap = ((GraphObjectMap)obj).toMap();

					for (final String colName : properties) {

						final Object value = convertedMap.get(colName);
						cell               = currentRow.createCell(cellCount++);

						writeToCell(factory, drawing, cell, value, maxCellLength, overflowMode);
					}

				} else if (obj instanceof GraphObject) {

					final GraphObject graphObj = (GraphObject)obj;

					for (final String colName : properties) {

						final PropertyKey key = graphObj.getTraits().key(colName);
						final Object value    = graphObj.getProperty(key);
						cell                  = currentRow.createCell(cellCount++);

						writeToCell(factory, drawing, cell, value, maxCellLength, overflowMode);
					}

				} else if (obj instanceof Map) {

					final Map map = (Map)obj;

					for (final String colName : properties) {

						final Object value = map.get(colName);
						cell               = currentRow.createCell(cellCount++);

						writeToCell(factory, drawing, cell, value, maxCellLength, overflowMode);
					}
				}
			}
		}

		return workbook;
	}

	public String escapeForExcel(final Object value) {

		String result;

		if (value == null) {

			result = "";

		} else if (value instanceof String[]) {

			List<String> quotedStrings = Arrays.asList((String[])value);
			result = quotedStrings.toString();

		} else if (value instanceof Collection) {

			// Special handling for collections of nodes
			ArrayList<String> quotedStrings = new ArrayList();
			for (final Object obj : (Collection)value) {
				quotedStrings.add(obj.toString());
			}

			result = quotedStrings.toString();

		} else if (value instanceof Date) {

			result = DatePropertyGenerator.format((Date) value, DateProperty.getDefaultFormat());

		} else {

			result = value.toString();

		}

		return result;
	}

	public void writeToCell(final CreationHelper factory, final Drawing drawing, final XSSFCell cell, final Object value, final Integer maxCellLength, final String overflowMode) {

		final String cellValue = escapeForExcel(value);

		if (cellValue.length() <= maxCellLength) {

			cell.setCellValue(cellValue);

		} else {

			cell.setCellValue(cellValue.substring(0, maxCellLength));

			if (!"t".equals(overflowMode)) {
				final Comment comment = drawing.createCellComment(factory.createClientAnchor());

				if ("o".equals(overflowMode)) {
					final String overflow = cellValue.substring(maxCellLength, Math.min(maxCellLength + 32767, cellValue.length()));
					comment.setString(factory.createRichTextString(overflow));
				} else {
					comment.setString(factory.createRichTextString(overflowMode));
				}

				cell.setCellComment(comment);
			}
		}
	}
}
