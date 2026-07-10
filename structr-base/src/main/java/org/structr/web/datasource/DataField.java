/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.datasource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.QueryGroup;
import org.structr.core.datasources.Channel;
import org.structr.core.datasources.ChannelResult;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.DataAdapterField;
import org.structr.core.function.TitleizeFunction;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.GraphSearchAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;

import java.text.SimpleDateFormat;
import java.util.*;

public class DataField extends LinkedHashMap<String, Object> {

	private static final Map<String, Integer> supportedDateFormatsForFiltering = new LinkedHashMap<>();

	static {

		// order from longest to shortest because parsing is lenient and some formats match multiple inputs
		supportedDateFormatsForFiltering.put("dd.MM.yyyy", Calendar.DAY_OF_YEAR);
		supportedDateFormatsForFiltering.put("dd.MM.", Calendar.DAY_OF_YEAR);
		supportedDateFormatsForFiltering.put("MM/yy", Calendar.MONTH);
		supportedDateFormatsForFiltering.put("MM/yyyy", Calendar.MONTH);
		supportedDateFormatsForFiltering.put("HH:mm", Calendar.HOUR_OF_DAY);
		supportedDateFormatsForFiltering.put("yy", Calendar.YEAR);
		supportedDateFormatsForFiltering.put("yyyy", Calendar.YEAR);
	}

	public DataField(final String name) {

		put("name", name);
	}

	public String getName() {
		return (String) get("name");
	}

	public String getLabel() {
		return (String) get("label");
	}

	public String getValue() {
		return (String)	get("value");
	}

	public String getTemplate() {
		return (String)	get("template");
	}

	public String getEditTemplate() {
		return (String)	get("editTemplate");
	}

	public String getPropertyName() {
		return (String)	get("propertyName");
	}

	public Set<String> getSlots() {
		return (Set<String>) get("slots");
	}

	public Map<String, Object> getOptions() {
		return (Map<String, Object>) get("options");
	}

	public Map<String, Object> getConfig() {
		return (Map<String, Object>) get("config");
	}

	public Boolean showLabel() {
		return (Boolean) get("showLabel");
	}

	public int getColumns() {

		final Integer cols = (Integer) get("columns");
		if (cols != null) {

			return cols.intValue();
		}

		// default number of columns is 6 for now...
		return 6;
	}

	public int getRows() {

		final Integer rows = (Integer) get("rows");
		if (rows != null) {

			return rows.intValue();
		}

		// default number of rows is 8 for now
		return 8;
	}

	public boolean isCollection() {

		final Boolean isCollection = (Boolean) get("isCollection");
		if (isCollection != null && isCollection.booleanValue()) {

			return true;
		}

		return false;
	}

	public boolean isSearchable() {

		final Boolean isSearchable = (Boolean) get("isSearchable");
		if (isSearchable != null && isSearchable.booleanValue()) {

			return true;
		}

		return false;
	}

	public String getSortKey() {

		if (containsKey("sortKey")) {
			return (String) get("sortKey");
		}

		if (containsKey("propertyName")) {
			return (String) get("propertyName");
		}

		return getName();
	}

	public String getSearchKey() {

		if (containsKey("propertyName")) {
			return (String) get("propertyName");
		}

		return getName();
	}

	public String getColumnDataSource() {
		return (String) get("columnDataSource");
	}

	public String getColumnKey() {
		return (String) get("columnKey");
	}

	public String getEditModeCondition() {
		return (String) get("editModeCondition");
	}

	public void applyCssClasses(final Set<String> cssClasses) {

		cssClasses.add("col-span-" + getColumns());
	}

	public void augment(final DataAdapterField augmentation) throws FrameworkException {

		putIfNotEmpty(this, "template",          augmentation.getRenderTemplate());
		putIfNotEmpty(this, "editTemplate",      augmentation.getEditTemplate());
		putIfNotEmpty(this, "label",             augmentation.getLabel());
		putIfNotEmpty(this, "value",             augmentation.getValue());
		putIfNotEmpty(this, "dataType",          augmentation.getDataType());
		putIfNotEmpty(this, "sortKey",           augmentation.getSortKey());
		putIfNotEmpty(this, "isSearchable",      augmentation.isSearchable());
		putIfNotEmpty(this, "rows",              augmentation.getRows());
		putIfNotEmpty(this, "columns",           augmentation.getColumns());
		putIfNotEmpty(this, "columnDataSource",  augmentation.getColumnDataSource());
		putIfNotEmpty(this, "columnKey",         augmentation.getColumnKey());
		putIfNotEmpty(this, "editModeCondition", augmentation.getEditModeCondition());

		// only adapter fields can be deleted in UI
		putIfAbsent("source", "adapter");

		// store field ID
		put("id", augmentation.getUuid());

		// detail config
		final Map<String, Object> config = augmentation.getConfig();
		if (config != null) {

			put("config", config);
		}
	}

	public void configureQuery(final Traits traits, final QueryGroup<NodeInterface> query, final String filterString) {

		final String searchKey = getSearchKey();
		if (traits.hasKey(searchKey)) {

			final PropertyKey key = traits.key(searchKey);

			if (String.class.equals(key.valueType())) {

				query.key(traits.key(searchKey), filterString, false);

			} else if (Number.class.isAssignableFrom(key.valueType())) {

				// if input string is not numeric, we don't search in numeric fields
				if (NumberUtils.isCreatable(filterString)) {

					query.key(key, NumberUtils.createNumber(filterString), false);
				}

			} else if (Date.class.equals(key.valueType())) {

				tryToParseDateQuery(query, key, filterString);

			} else if (key.relatedType() != null) {

				// we can only use the name of the related node for filtering right now..
				query.add(new GraphSearchAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), key, filterString, false));

			} else {

				//System.out.println("Don't know how to handle property type " + key.valueType() + " in component search.");
			}

		} else {

			//System.out.println("Unable to use search field " + getName() + ", no property found.");
		}
	}

	public Map<String, GraphObject> expandColumns(final RenderContext renderContext) throws FrameworkException {

		final Map<String, GraphObject> columns = new LinkedHashMap<>();
		final String dataSourceName            = getColumnDataSource();
		final String label                     = getLabel();
		final Channel<GraphObject> channel     = Channel.forName(dataSourceName);

		if (channel != null) {

			// evaluate column query, no transform here (maybe we can add one in the future)
			final ChannelResult<GraphObject> data = channel.getResult(renderContext, null);

			for (final Object item : data.getData()) {

				if (item instanceof GraphObject g) {

					renderContext.setConstant(getColumnKey(), g);

					final Object expandedLabel = Scripting.evaluate(renderContext, g, "${" + label + "}", "Label expression of data field '" + getName() + "'");
					if (expandedLabel != null) {

						columns.put(expandedLabel.toString(), g);
					}
				}
			}

		} else {

			// default case: only one column
			columns.put(label, null);
		}

		return columns;
	}

	// ----- public static methods -----
	public static DataField from(final RenderContext renderContext, final DataAdapter adapter, final String name, final FieldDefinition fieldDefinition, final DataAdapterField augmentation, final boolean loadOptions) throws FrameworkException {

		final DataField field = new DataField(name);

		field.put("propertyName", name);
		field.put("value",        adapter.getDataKey() + "." + name);
		field.put("label",        TitleizeFunction.titleize(CaseHelper.toUnderscore(name, false), "_"));
		field.put("template",     "span");

		// data from field definition
		if (fieldDefinition != null) {

			field.put("isSearchable",   fieldDefinition.isIndexed());
			field.put("required",       fieldDefinition.isRequired());
			field.put("multiple",       fieldDefinition.isCollection());

			// don't overwrite default with null
			if (fieldDefinition.renderTemplate() != null) {
				field.put("template", fieldDefinition.renderTemplate());
			}

			field.put("editTemplate",   fieldDefinition.editTemplate());
			field.put("dataType",       fieldDefinition.dataType());
			field.put("isCollection",   fieldDefinition.isCollection());
			field.put("source",         "datasource");

			if (loadOptions && fieldDefinition.hasOptions()) {

				String label = "name";
				String filter = null;

				// check options that transform schema info
				final Map<String, Object> options = null;//augmentation.getOptions();
				if (options != null && !options.isEmpty()) {

					if (options.containsKey("label")) {
						label = (String) options.get("label");
					}

					filter = (String) options.get("filter");
				}

				field.put("options", fieldDefinition.getOptions(renderContext, filter, label));
			}

		} else {

			field.put("dataType", "custom");
		}

		// data from augmentation field
		if (augmentation != null) {

			field.augment(augmentation);
		}

		return field;

	}

	// ----- private static methods -----
	private static void putIfNotEmpty(final DataField field, final String key, final Object value) {

		if (value != null && StringUtils.isNotBlank(value.toString())) {
			field.put(key, value);
		}
	}

	// ----- private methods -----
	private void tryToParseDateQuery(final QueryGroup<NodeInterface> query, final PropertyKey key, final String input) {

		final Calendar now = GregorianCalendar.getInstance();

		// date format needs to be synchronized
		for (final String format : supportedDateFormatsForFiltering.keySet()) {

			try {

				final SimpleDateFormat parser = new SimpleDateFormat(format);
				final Calendar calendar       = GregorianCalendar.getInstance();
				final Integer field           = supportedDateFormatsForFiltering.get(format);

				calendar.setTime(parser.parse(input));

				// some corrections for edge cases
				if (field == Calendar.DAY_OF_YEAR && calendar.get(Calendar.YEAR) == 1970) {
					// pattern dd.MM. causes the year to be set to 1970
					calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
				}

				if (field == Calendar.HOUR_OF_DAY && calendar.get(Calendar.YEAR) == 1970) {
					// pattern HH:mm causes year, month and date to be set to 1970
					calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
					calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
					calendar.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR));
				}

				final Date rangeStart = calendar.getTime();

				calendar.add(field, 1);

				final Date rangeEnd = calendar.getTime();

				query.range(key, rangeStart, rangeEnd);

				// first match wins
				return;

			} catch (Throwable ignore) {}
		}
	}
}
