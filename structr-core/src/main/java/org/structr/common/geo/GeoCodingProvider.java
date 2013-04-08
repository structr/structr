package org.structr.common.geo;

import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */

public interface GeoCodingProvider {

	public GeoCodingResult geocode(final String street, final String house, String postalCode, final String city, final String state, final String country, String language) throws FrameworkException;
}
