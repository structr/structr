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
package org.structr.ui.page.admin;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Checkbox;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Option;
import org.apache.click.control.Select;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.DoubleField;
import org.apache.click.extras.control.IntegerField;
import org.apache.click.util.Bindable;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.geo.Map;

/**
 *
 * @author amorgner
 */
public class EditMap extends EditGeoObject {

    protected Map map;
    @Bindable
    protected ActionLink clearCacheLink = new ActionLink("Clear Cache", this, "onClearCache");

    public EditMap() {

        super();

        FieldSet generalFields = new FieldSet("General");
//        generalFields.setColumns(2);
        //mapFields.add(new TextField(Map.SHAPEFILE_KEY));
        generalFields.add(new TextField(Map.Key.layer.name()));
        generalFields.add(new Checkbox(Map.Key.isStatic.name()));
        generalFields.add(new TextField(Map.Key.staticFeatureName.name()));
        generalFields.add(new TextField(Map.Key.featureNameParamName.name()));
        generalFields.add(new Checkbox(Map.Key.dontCache.name()));
        generalFields.add(clearCacheLink);
        generalFields.add(new TextField(Map.Key.contentType.name(), "Internet Media Type (Content-Type)", 30));
        editPropertiesForm.add(generalFields);

        FieldSet displayFields = new FieldSet("Display");
        displayFields.add(new Checkbox(Map.Key.displayCities.name()));
        editPropertiesForm.add(displayFields);

        FieldSet canvasFields = new FieldSet("Canvas (Browser Display Area)");
        canvasFields.setColumns(2);
        canvasFields.add(new IntegerField(Map.Key.canvasX.name()));
        canvasFields.add(new IntegerField(Map.Key.canvasY.name()));
        editPropertiesForm.add(canvasFields);

        FieldSet manEnvFields = new FieldSet("Envelope (Map Bounding Area) Dimensions");
        manEnvFields.setColumns(2);
        manEnvFields.add(new DoubleField(Map.Key.envelopeMinX.name()));
        manEnvFields.add(new DoubleField(Map.Key.envelopeMaxX.name()));
        manEnvFields.add(new DoubleField(Map.Key.envelopeMinY.name()));
        manEnvFields.add(new DoubleField(Map.Key.envelopeMaxY.name()));
        manEnvFields.add(new Checkbox(Map.Key.autoEnvelope.name()));
        editPropertiesForm.add(manEnvFields);

        FieldSet styleFields = new FieldSet("Polygon Style");
        styleFields.setColumns(3);
        styleFields.add(new TextField(Map.Key.fillColor.name()));
        styleFields.add(new DoubleField(Map.Key.fillOpacity.name()));
        styleFields.add(new IntegerField(Map.Key.lineWidth.name()));
        styleFields.add(new TextField(Map.Key.lineColor.name()));
        styleFields.add(new DoubleField(Map.Key.lineOpacity.name()));
        editPropertiesForm.add(styleFields);

        FieldSet pointFields = new FieldSet("Point Style");
        pointFields.setColumns(3);
        Select shapeSelect = new Select(Map.Key.pointShape.name());
        shapeSelect.add(new Option("Circle"));
        shapeSelect.add(new Option("Square"));
        shapeSelect.add(new Option("Cross"));
        shapeSelect.add(new Option("X"));
        shapeSelect.add(new Option("Triangle"));
        shapeSelect.add(new Option("Star"));
        pointFields.add(shapeSelect);
        pointFields.add(new IntegerField(Map.Key.pointDiameter.name()));
        pointFields.add(new TextField(Map.Key.pointFillColor.name()));
        pointFields.add(new DoubleField(Map.Key.pointFillOpacity.name()));
        pointFields.add(new IntegerField(Map.Key.pointStrokeLineWidth.name()));
        pointFields.add(new TextField(Map.Key.pointStrokeColor.name()));
        pointFields.add(new TextField(Map.Key.pointFontName.name()));
        pointFields.add(new DoubleField(Map.Key.pointFontSize.name()));
        pointFields.add(new TextField(Map.Key.pointFontColor.name()));
        pointFields.add(new DoubleField(Map.Key.pointFontOpacity.name()));
        editPropertiesForm.add(pointFields);

        FieldSet labelFields = new FieldSet("Label Style");
        labelFields.setColumns(2);
        labelFields.add(new TextField(Map.Key.fontName.name()));
        labelFields.add(new DoubleField(Map.Key.fontSize.name()));
        labelFields.add(new TextField(Map.Key.fontColor.name()));
        labelFields.add(new DoubleField(Map.Key.fontOpacity.name()));
        labelFields.add(new DoubleField(Map.Key.labelAnchorX.name()));
        labelFields.add(new DoubleField(Map.Key.labelAnchorY.name()));
        labelFields.add(new DoubleField(Map.Key.labelDisplacementX.name()));
        labelFields.add(new DoubleField(Map.Key.labelDisplacementY.name()));
        editPropertiesForm.add(labelFields);

        FieldSet optFields = new FieldSet("Optimization Parameter");
        optFields.setColumns(2);
        optFields.add(new Checkbox(Map.Key.lineWidthOptimization.name()));
        optFields.add(new Checkbox(Map.Key.optimizeFtsRendering.name()));
        editPropertiesForm.add(optFields);

    }

    @Override
    public void onInit() {

        super.onInit();

        if (node != null) {
            clearCacheLink.setParameter(AbstractNode.Key.nodeId.name(), getNodeId());
            map = (Map) node;
        }

    }

    /**
     * Save form data and stay in edit mode
     *
     * @return
     */
    @Override
    public boolean onSaveProperties() {

        clearCache();
        return super.onSaveProperties();

    }

    /**
     * Clear map cache by setting the SVG content to null
     * 
     * @return
     */
    public boolean onClearCache() {

        clearCache();
        return redirect();

    }

    private void clearCache() {
        if (node != null) {
            map = (Map) node;
            map.setSvgContent(null);
        }
    }
}
