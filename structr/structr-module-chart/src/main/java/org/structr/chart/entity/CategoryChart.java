/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.chart.entity;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;

import org.structr.chart.renderer.BarChartRenderer;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public abstract class CategoryChart extends Chart {

	static {

		EntityContext.registerPropertySet(CategoryChart.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {
		orientation, categoryAxisLabel, valueAxisLabel, categoryProperty, valueProperty, recursive
	}

	//~--- methods --------------------------------------------------------

	public abstract void configureCategoryAxis(CategoryAxis categoryAxis);

	public abstract void configureValueAxis(ValueAxis valueAxis);

	public abstract void configurePlot(CategoryPlot plot);

	public abstract void configureChart(JFreeChart chart);

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new BarChartRenderer());
		renderers.put(RenderMode.Direct,
			      new BarChartRenderer());
	}

	//~--- get methods ----------------------------------------------------

	public abstract CategoryItemRenderer getCategoryItemRenderer();

	public String getOrientation() {
		return (getStringProperty(Key.orientation));
	}

	public String getCategroyAxisLabel() {
		return (getStringProperty(Key.categoryAxisLabel));
	}

	public String getValueAxisLabel() {
		return (getStringProperty(Key.valueAxisLabel));
	}

	public boolean getRecursive() {
		return (getBooleanProperty(Key.recursive));
	}
}
