package org.structr.rest.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.AddressComponent;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import static org.structr.core.entity.AbstractNode.name;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionNotionProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Christian Morgner
 */
public class TestNine extends AbstractNode {

	public static final CollectionProperty<TestEight> testEights   = new CollectionProperty<TestEight>("testEights", TestEight.class, TestRestRelType.HAS, false);
	public static final Property<List<String>>        testEightIds = new CollectionNotionProperty("testEightIds", testEights, new PropertyNotion(GraphObject.uuid));
	
	public static final Property<String>              city         = new StringProperty("city").indexed().indexedWhenEmpty();
	public static final Property<String>              street       = new StringProperty("street").indexed().indexedWhenEmpty();
	public static final Property<String>              postalCode   = new StringProperty("postalCode").indexed().indexedWhenEmpty();
	               
	public static final Property<Double>              latitude     = new DoubleProperty("latitude");
	public static final Property<Double>              longitude    = new DoubleProperty("longitude");
	
	public static final View defaultView = new View(TestNine.class, PropertyView.Public,
		name, city, street, postalCode, latitude, longitude
	);

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		geocode();
		
		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		
		geocode();
		
		return super.onModification(securityContext, errorBuffer);
	}

	public void geocode() throws FrameworkException {

		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Double lat              = getProperty(latitude);
				Double lon              = getProperty(longitude);

				if (lat == null || lon == null) {

					String _city       = getProperty(city);
					String _street     = getProperty(street);
					String _postalCode = getProperty(postalCode);

					GeoCodingResult geoCodingResult = GeoHelper.geocode(_street, null, _postalCode, _city, null, null);
					if (geoCodingResult == null) {

						return null;

					}

					setProperty(latitude, geoCodingResult.getLatitude());
					setProperty(longitude, geoCodingResult.getLongitude());
					
					// set postal code if found
					AddressComponent postalCodeComponent = geoCodingResult.getAddressComponent(GeoCodingResult.Type.postal_code);
					if (postalCodeComponent != null) {
						
						setProperty(postalCode, postalCodeComponent.getLongValue());
					}
				}

				return null;
			}
		});
	}
}
