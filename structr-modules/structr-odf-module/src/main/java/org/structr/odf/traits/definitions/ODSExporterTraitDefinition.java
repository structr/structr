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
package org.structr.odf.traits.definitions;

import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.odf.entity.ODSExporter;
import org.structr.odf.traits.wrappers.ODSExporterTraitWrapper;
import org.structr.schema.action.EvaluationHints;
import org.structr.storage.StorageProviderFactory;
import org.structr.transform.VirtualType;
import org.structr.web.entity.File;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 */
public class ODSExporterTraitDefinition extends AbstractNodeTraitDefinition {

	public ODSExporterTraitDefinition() {
		super("ODSExporter");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ODSExporter.class, (traits, node) -> new ODSExporterTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("exportAttributes", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final Map<String, Object> data = arguments.toMap();
					final String uuid              = (String)data.get("uuid");

					exportAttributes(securityContext, entity.as(ODSExporter.class), uuid);
					return null;
				}
			}
		);
	}

	public void exportAttributes(final SecurityContext securityContext, final ODSExporter entity, final String uuid) throws FrameworkException {

		final File output                = entity.getResultDocument();
		final VirtualType transformation = entity.getTransformationProvider();

		try {

			final App app = StructrApp.getInstance();
			final ResultStream result = app.nodeQuery(StructrTraits.NODE_INTERFACE).and(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.ID_PROPERTY), uuid).getResultStream();
			final ResultStream transformedResult = transformation.transformOutput(securityContext, StructrTraits.NODE_INTERFACE, result);

			Map<String, Object> nodeProperties = new HashMap<>();
			GraphObjectMap node = (GraphObjectMap) Iterables.first(transformedResult);
			node.getPropertyKeys(null).forEach(
				p -> nodeProperties.put(p.dbName(), node.getProperty(p))
			);

			OdfSpreadsheetDocument spreadsheet = OdfSpreadsheetDocument.loadDocument(StorageProviderFactory.getStorageProvider(output).getInputStream());
			OdfTable sheet = spreadsheet.getTableList().get(0);

			Iterator<Entry<String, Object>> it = nodeProperties.entrySet().iterator();

			while (it.hasNext()) {

				Entry<String, Object> currentEntry = it.next();
				String address = currentEntry.getKey();
				Object val = currentEntry.getValue();

				if (val instanceof Collection) {

					Collection col = (Collection) val;
					writeCollectionToCells(sheet, sheet.getCellByPosition(address), col);

				} else if (val instanceof String[]) {

					String[] arr = (String[]) val;
					List<String> list = new ArrayList<>(Arrays.asList(arr));
					writeCollectionToCells(sheet, sheet.getCellByPosition(address), list);

				} else {
					writeObjectToCell(sheet.getCellByPosition(address), val);
				}

			}

			spreadsheet.save(StorageProviderFactory.getStorageProvider(output).getOutputStream());
			spreadsheet.close();

		} catch (Exception e) {
			final Logger logger = LoggerFactory.getLogger(ODSExporterTraitDefinition.class);
			logger.error("Error while exporting to ODS", e);
		}
	}


	public static void writeCollectionToCells(final OdfTable sheet, final OdfTableCell startCell, final Collection col) {

		int rowIndex, colIndex;

		colIndex = startCell.getColumnIndex();
		rowIndex = startCell.getRowIndex();

		Iterator<Collection> colIt = col.iterator();

		while (colIt.hasNext()) {
			Object obj = colIt.next();
			if (obj instanceof String[]) {

				String[] arr = (String[]) obj;
				List<String> list = new ArrayList<>(Arrays.asList(arr));
				StringJoiner sj = new StringJoiner(",");
				list.forEach(
					s -> sj.add(s)
				);
				writeObjectToCell(sheet.getCellByPosition(colIndex, rowIndex), sj.toString());

			} else if (obj instanceof Collection) {

				Collection nestedCol = (Collection) obj;
				StringJoiner sj = new StringJoiner(",");
				nestedCol.forEach(
					s -> sj.add(s.toString())
				);
				writeObjectToCell(sheet.getCellByPosition(colIndex, rowIndex), sj.toString());

			} else {

				writeObjectToCell(sheet.getCellByPosition(colIndex, rowIndex), obj);

			}

			rowIndex++;
		}

	}

	public static void writeObjectToCell(final OdfTableCell cell, final Object val) {

		if (val instanceof String) {

			cell.setStringValue((String) val);

		} else if (val instanceof Integer) {

			Integer i = (Integer) val;
			cell.setDoubleValue(i.doubleValue());

		} else if (val instanceof Double) {

			cell.setDoubleValue((Double) val);

		} else if (val instanceof Boolean) {

			cell.setBooleanValue((Boolean) val);

		} else if (val instanceof AbstractNode) {

			AbstractNode node = (AbstractNode) val;
			cell.setStringValue(
				node.getProperty(new StringProperty("id"))
			);

		} else if (val != null) {

			cell.setStringValue(val.toString());

		}
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
