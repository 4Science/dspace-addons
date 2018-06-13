package org.dspace.app.webui.servlet;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

public class IIIFAuthenticationUtils {
	private static Logger log = Logger.getLogger(IIIFAuthenticationUtils.class);
	private static String clickthrough = ConfigurationManager.getProperty("iiif", "clickthrough");
	private static String clickthrough_metadata = ConfigurationManager.getProperty("iiif", "clickthrough.metadata");
	private static String clickthrough_default_terms = null;
	
	static {
		try {
			String clickthrough_file = ConfigurationManager.getProperty("iiif", "clickthrough.file");
			FileInputStream inputStream = new FileInputStream(clickthrough_file);
			clickthrough_default_terms = IOUtils.toString(inputStream);
			inputStream.close();
		} catch (IOException e) {
			log.warn(e.getMessage());
		}
	}

	public static String clickthroughTermOfUsage(Item item) {
		boolean use_default_terms = false;
		String clickthrough_metadata_value = null;
		try {
			clickthrough_metadata_value = item.getMetadata(clickthrough_metadata);
		} catch (NullPointerException e) {
			log.warn(e.getMessage(), e);
		}
		if (StringUtils.equals(clickthrough, "all")) {
			use_default_terms = true;
		} else if (StringUtils.equals(clickthrough, "item")) {
			if (StringUtils.isBlank(clickthrough_metadata_value)) {
				use_default_terms = true;
			} else if (!StringUtils.equals(clickthrough_metadata_value, "none")) {
			}
		}
		
		if (!StringUtils.equalsIgnoreCase(clickthrough, "none")
				&& !StringUtils.equalsIgnoreCase(clickthrough_metadata_value, "none")) {
			if (use_default_terms) {
				return clickthrough_default_terms;
			} else {
				return clickthrough_metadata_value;
			}
		}
		return null;
	}
}
