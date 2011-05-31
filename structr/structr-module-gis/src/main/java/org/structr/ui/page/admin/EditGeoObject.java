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

import org.apache.click.control.FieldSet;
import org.apache.click.extras.control.DoubleField;
import org.apache.click.util.Bindable;
import org.structr.core.entity.geo.GeoObject;
import org.structr.ui.page.admin.DefaultEdit;

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
