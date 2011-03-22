/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.ActionLink;
import org.apache.click.control.Checkbox;
import org.apache.click.control.FieldSet;
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
        editPropertiesForm.add(generalFields);

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

        FieldSet labelFields = new FieldSet("Label Style");
        labelFields.setColumns(2);
        labelFields.add(new TextField(Map.FONT_NAME_KEY));
        labelFields.add(new DoubleField(Map.FONT_SIZE_KEY));
        labelFields.add(new TextField(Map.FONT_COLOR_KEY));
        labelFields.add(new DoubleField(Map.FONT_OPACITY_KEY));
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
            clearCacheLink.setParameter(AbstractNode.NODE_ID_KEY, getNodeId());
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
