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
import org.structr.common.error.ArgumentTypeException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.property.DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.schema.parser.DatePropertyGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ToExcelFunction extends Function<Object, Object> {

	@Override
	public String getName() {

		return "toExcel";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("data_1, config_1, data_2, config_2, ..., data_N, config_N");
	}

	@Override
	public String getRequiredModule() {

		return "excel";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			final int parameterCount = sources.length;
			if (parameterCount < 2) {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getDisplayName(false) + ": Expected at least 2 parameters, got "  + parameterCount);
			}

			if (parameterCount % 2 != 0) {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getDisplayName(false) + ": Expected argument count to be even, got " + parameterCount);
			}

			try (final Workbook workbook = new XSSFWorkbook()) {

				final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

				for (int c = 0; c < parameterCount; c += 2) {

					if (sources[c] instanceof Iterable it) {

						final List<GraphObject> nodes = Iterables.toList(it);
						final ToExcelOptions options = ToExcelOptions.from(getName(), sources, c + 1).validate(getName());
						String propertyView = options.getPropertyView();
						List<String> properties = options.getProperties();

						addSheetToWorkbook(workbook, options.getSheetName(), nodes, propertyView, properties, options.getIncludeHeader(), options.getLocalizeHeader(), options.getLocalizationDomain(), ctx.getLocale(), options.getMaxCellLength(), options.getOverflowMode());

					} else {

						return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, getDisplayName(false) + ": Parameter " + (c+1) + " must be a collection!");
					}
				}

				workbook.write(outputStream);

				return outputStream.toString(StandardCharsets.ISO_8859_1);

			} catch (final IOException e) {

				logger.warn("{}(): IOException occurred", getName(), e);

				return null;
			}

		} catch (ArgumentTypeException e) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, e.getMessage());

		} catch (IllegalArgumentException e) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, e.getMessage(), e);
		}
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("data_1", "Collection of objects (these objects can be database nodes or javascript objects)"), Parameter.mandatory("config_1",
						"""
								configuration for the previous data parameter\
								\
								<ul>\
									<li> `sheetName`: name of the data sheet. Sheets are named consecutively starting at 'Sheet0', if missing. For Excel compliance, sheet names are trimmed at 31 characters.</li>\
									<li> `view`: name of a view. For this to work, the matching data collection must be database nodes. (mutually exclusive with `properties` - exactly one must be given)</li>\
									<li> `properties`: collection of property names. Works with data being database nodes and custom objects. (mutually exclusive with `view` - exactly one must be given)</li>\
									<li> `includeHeader`: indicates if a header row should be included (default: `true`)</li>\
									<li> `localizeHeader`: indicates if the column names in the header should be localized (default: `false`)</li>\
									<li> `localizationDomain`: lookup domain for localization of header names (default ``)</li>\
									<li> `maxCellLength`: maximum length after which content cells overflow (default and maximum: 32767)</li>\
									<li> `overflowMode`: Controls how content that is longer than `maxCellLength` is handled (affects content-rows only - the header remains untouched)\
										<ul>\
											<li> `t`: Truncates the content</li>\
											<li> `o` (default): Overflows the remaining text into a cell comment. (This is restricted to 32767 bytes by Excel)</li>\
											<li> For any other value, the cell is truncated and this value is used as a cell comment (Useful to display a message like "The content of this cell has been truncated").</li>\
										</ul>\
									</li>\
								</ul>\
								"""
				), Parameter.optional("data_N", "same as above"), Parameter.optional("config_N", "same as above"));
	}

	@Override
	public String getShortDescription() {

		return "Creates Excel from given data and configuration.";
	}

	@Override
	public String getLongDescription() {

		return "Returns XLSX string representation of the given data and configuration.";
	}

	@Override
	public List<Example>getExamples() {

		return List.of(
				Example.structrScript("""
				${
					setContent(
						create('File', 'name', 'users.xlsx'),
						toExcel(
							find('User'), { view: 'public', sheetName: 'Users' },
						),
						'ISO-8859-1'
					)
				}\
				""", "Excel file with view 'public' of user list."),
				Example.structrScript("""
				${(
					setResponseHeader('Content-Disposition', 'attachment; filename="users-and-projects.xlsx"'),
					setResponseHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=ISO-8859-1', true),

					toExcel(
						find('User'), { view: 'public', sheetName: 'Users' },
						find('Project'), { properties: ['id', 'type', 'name'], sheetName: 'Pages' }
					)
				)}\
				""", "Downloadable dynamic file containing the list of users and pages"),
				Example.javaScript("""
				${{
					$.setResponseHeader('Content-Disposition', 'attachment; filename="users-and-projects.xlsx"');
					$.setResponseHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=ISO-8859-1', true);

					$.print(
						$.toExcel(
							$.find('User'), { view: 'public', sheetName: 'Users' },
							$.find('Project'), { view: 'public', sheetName: 'Projects' }
						)
					);
				}}\
				""", "Downloadable dynamic file with view 'public' of user list of users"),
				Example.javaScript("""
				${{
					$.setResponseHeader('Content-Disposition', 'attachment; filename="users-and-projects.xlsx"');
					$.setResponseHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=ISO-8859-1', true);

					$.print(
						$.toExcel(
							$.find('User'), { view: 'excelView', sheetName: 'Users', maxCellLength: 1000, overflowMode: 't' },
							$.find('Project'), { view: 'excelView', sheetName: 'Projects', maxCellLength: 1000, overflowMode: 't' }
						)
					);
				}}\
				""", "Downloadable dynamic file with view 'excelView' of user and project list where all cells are truncated after 1000 characters.")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
				"For each `data` parameter, a matching `config` parameter must be present",
				"If the `data` collection is empty and a `view` option is given, no header row can be printed because the view is identified via the first element",
				"The output of this function is the string representation of a complete excel file. Additional data can not be appended later on",
				"The `contentType` of the dynamic file needs to have `charset=ISO-8859-1` (e.g. `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=ISO-8859-1`) or override its contentType in code like done in the examples",
				"Ensure there is no extraneous whitespace after the dynamic script content, otherwise Excel will report the file as corrupt.",
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

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}

	public void addSheetToWorkbook(final Workbook workbook, final String sheetName, final List list, final String propertyView, final List<String> properties, final boolean includeHeader, final boolean localizeHeader, final String headerLocalizationDomain, final Locale locale, final Integer maxCellLength, final String overflowMode) throws IOException {

		final CreationHelper factory = workbook.getCreationHelper();
		final XSSFSheet sheet        = (XSSFSheet) (sheetName == null ? workbook.createSheet() : workbook.createSheet(sheetName));
		final Drawing drawing        = sheet.createDrawingPatriarch();
		int rowCount  = 0;
		int cellCount = 0;
		XSSFRow currentRow = null;
		XSSFCell cell      = null;

		if (includeHeader) {

			currentRow = sheet.createRow(rowCount++);
			cellCount  = 0;

			if (propertyView != null) {

				if (list.isEmpty()) {

					// empty lists are not an error case. even though we can not print the header names when a view name is given

					return;
				}

				final Object obj = list.getFirst();
				if (obj instanceof GraphObject) {

					if (!((GraphObject)obj).getTraits().getViewNames().contains(propertyView)) {

						logger.warn("{}(): View '{}' does not exist for graph object {}", getName(), propertyView, obj);

						return;
					}

					for (final PropertyKey key : ((GraphObject)obj).getPropertyKeys(propertyView)) {

						cell = currentRow.createCell(cellCount++);

						String value = key.dbName();

						if (localizeHeader) {

							try {

								value = LocalizeFunction.getLocalization(locale, value, headerLocalizationDomain);

							} catch (FrameworkException fex) {

								logger.warn("{}(): Exception", getName(), fex);
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

							logger.warn("{}(): Exception", getName(), fex);
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
					final Traits traits = graphObj.getTraits();

					for (final String colName : properties) {

						cell         = currentRow.createCell(cellCount++);
						Object value = "";

						if (traits.hasKey(colName)) {

							value = graphObj.getProperty(traits.key(colName));

						} else {

							logger.warn("{}(): Node of type '{}' does not have attribute '{}' - writing empty string.", getName(), traits.getName(), colName);
						}

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
