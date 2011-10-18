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

import org.structr.common.PropertyView;
import org.structr.core.EntityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class StackedBarChart extends CategoryChart {

	static {

		EntityContext.registerPropertySet(StackedBarChart.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void configureCategoryAxis(CategoryAxis categoryAxis) {}

	@Override
	public void configureValueAxis(ValueAxis valueAxis) {}

	@Override
	public void configurePlot(CategoryPlot plot) {}

	@Override
	public void configureChart(JFreeChart chart) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public CategoryItemRenderer getCategoryItemRenderer() {
		return (new org.jfree.chart.renderer.category.StackedBarRenderer());
	}

	@Override
	public String getIconSrc() {
		return "/images/chart_bar.png";
	}
}
