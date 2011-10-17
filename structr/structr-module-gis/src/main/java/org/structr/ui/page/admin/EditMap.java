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
        generalFields.add(new TextField(Map.LAYER_KEY));
        generalFields.add(new Checkbox(Map.STATIC_KEY));
        generalFields.add(new TextField(Map.STATIC_FEATURE_NAME_KEY));
        generalFields.add(new TextField(Map.FEATURE_NAME_PARAM_NAME_KEY));
        generalFields.add(new Checkbox(Map.DONT_CACHE_KEY));
        generalFields.add(clearCacheLink);
        generalFields.add(new TextField(Map.CONTENT_TYPE_KEY, "Internet Media Type (Content-Type)", 30));
        editPropertiesForm.add(generalFields);

        FieldSet displayFields = new FieldSet("Display");
        displayFields.add(new Checkbox(Map.DISPLAY_CITIES_KEY));
        editPropertiesForm.add(displayFields);

        FieldSet canvasFields = new FieldSet("Canvas (Browser Display Area)");
        canvasFields.setColumns(2);
        canvasFields.add(new IntegerField(Map.CANVAS_X_KEY));
        canvasFields.add(new IntegerField(Map.CANVAS_Y_KEY));
        editPropertiesForm.add(canvasFields);

        FieldSet manEnvFields = new FieldSet("Envelope (Map Bounding Area) Dimensions");
        manEnvFields.setColumns(2);
        manEnvFields.add(new DoubleField(Map.ENVELOPE_MIN_X_KEY));
        manEnvFields.add(new DoubleField(Map.ENVELOPE_MAX_X_KEY));
        manEnvFields.add(new DoubleField(Map.ENVELOPE_MIN_Y_KEY));
        manEnvFields.add(new DoubleField(Map.ENVELOPE_MAX_Y_KEY));
        manEnvFields.add(new Checkbox(Map.AUTO_ENVELOPE_KEY));
        editPropertiesForm.add(manEnvFields);

        FieldSet styleFields = new FieldSet("Polygon Style");
        styleFields.setColumns(3);
        styleFields.add(new TextField(Map.FILL_COLOR_KEY));
        styleFields.add(new DoubleField(Map.FILL_OPACITY_KEY));
        styleFields.add(new IntegerField(Map.LINE_WIDTH_KEY));
        styleFields.add(new TextField(Map.LINE_COLOR_KEY));
        styleFields.add(new DoubleField(Map.LINE_OPACITY_KEY));
        editPropertiesForm.add(styleFields);

        FieldSet pointFields = new FieldSet("Point Style");
        pointFields.setColumns(3);
        Select shapeSelect = new Select(Map.POINT_SHAPE_KEY);
        shapeSelect.add(new Option("Circle"));
        shapeSelect.add(new Option("Square"));
        shapeSelect.add(new Option("Cross"));
        shapeSelect.add(new Option("X"));
        shapeSelect.add(new Option("Triangle"));
        shapeSelect.add(new Option("Star"));
        pointFields.add(shapeSelect);
        pointFields.add(new IntegerField(Map.POINT_DIAMETER_KEY));
        pointFields.add(new TextField(Map.POINT_FILL_COLOR_KEY));
        pointFields.add(new DoubleField(Map.POINT_FILL_OPACITY_KEY));
        pointFields.add(new IntegerField(Map.POINT_STROKE_LINE_WIDTH_KEY));
        pointFields.add(new TextField(Map.POINT_STROKE_COLOR_KEY));
        pointFields.add(new TextField(Map.POINT_FONT_NAME_KEY));
        pointFields.add(new DoubleField(Map.POINT_FONT_SIZE_KEY));
        pointFields.add(new TextField(Map.POINT_FONT_COLOR_KEY));
        pointFields.add(new DoubleField(Map.POINT_FONT_OPACITY_KEY));
        editPropertiesForm.add(pointFields);

        FieldSet labelFields = new FieldSet("Label Style");
        labelFields.setColumns(2);
        labelFields.add(new TextField(Map.FONT_NAME_KEY));
        labelFields.add(new DoubleField(Map.FONT_SIZE_KEY));
        labelFields.add(new TextField(Map.FONT_COLOR_KEY));
        labelFields.add(new DoubleField(Map.FONT_OPACITY_KEY));
        labelFields.add(new DoubleField(Map.LABEL_ANCHOR_X_KEY));
        labelFields.add(new DoubleField(Map.LABEL_ANCHOR_Y_KEY));
        labelFields.add(new DoubleField(Map.LABEL_DISPLACEMENT_X_KEY));
        labelFields.add(new DoubleField(Map.LABEL_DISPLACEMENT_Y_KEY));
        editPropertiesForm.add(labelFields);

        FieldSet optFields = new FieldSet("Optimization Parameter");
        optFields.setColumns(2);
        optFields.add(new Checkbox(Map.LINE_WIDTH_OPTIMIZATION_KEY));
        optFields.add(new Checkbox(Map.OPTIMIZE_FTS_RENDERING_KEY));
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
