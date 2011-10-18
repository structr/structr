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

import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.PieDataset;

import org.structr.chart.renderer.PieChartRenderer;
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
public abstract class AbstractPieChart extends Chart {

	static {

		EntityContext.registerPropertySet(AbstractPieChart.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ label, valueProperty, recursive }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new PieChartRenderer());
		renderers.put(RenderMode.Direct,
			      new PieChartRenderer());
	}

	//~--- get methods ----------------------------------------------------

	public abstract PiePlot getPiePlot(PieDataset dataSet);

	public String getLabel() {
		return (getStringProperty(Key.label));
	}

	public String getValueProperty() {
		return (getStringProperty(Key.valueProperty));
	}

	public boolean getRecursive() {
		return (getBooleanProperty(Key.recursive));
	}

	@Override
	public String getIconSrc() {
		return "/images/chart_pie.png";
	}
}
