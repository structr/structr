package org.structr.common.geo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.jxpath.JXPathContext;
import org.structr.core.Services;

/**
 * Abstract base class for geocoding providers.
 *
 * @author Christian Morgner
 */
public abstract class AbstractGeoCodingProvider implements GeoCodingProvider {

	private static final Logger logger = Logger.getLogger(AbstractGeoCodingProvider.class.getName());
	protected String apiKey            = null;
	
	public AbstractGeoCodingProvider() {
		this.apiKey = Services.getConfigurationValue(Services.GEOCODING_APIKEY, "");
	}
	
	protected <T> T extract(Map source, String path, Class<T> type) {
		
		JXPathContext context = JXPathContext.newContext(source);
		T value               = (T)context.getValue(path);
		
		return value;
	}
	
	protected String encodeURL(String source) {
		
		try {
			return URLEncoder.encode(source, "UTF-8");
			
		} catch (UnsupportedEncodingException ex) {

			logger.log(Level.WARNING, "Unsupported Encoding", ex);
		}
		
		// fallback, unencoded
		return source;
	}
}
