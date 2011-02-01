/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ui.page.admin;

import org.apache.click.control.FieldSet;
import org.apache.click.extras.control.DoubleField;
import org.apache.click.util.Bindable;
import org.structr.core.entity.geo.GeoObject;

/**
 *
 * @author amorgner
 */
public class EditGeoObject extends DefaultEdit {

    @Bindable
    protected FieldSet geoFields = new FieldSet("Geo Data");

    public EditGeoObject() {

        super();

        geoFields.setColumns(2);
        geoFields.add(new DoubleField(GeoObject.LONGITUDE_KEY));
        geoFields.add(new DoubleField(GeoObject.LATITUDE_KEY));
        editPropertiesForm.add(geoFields);
    }

}
