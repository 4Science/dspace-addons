/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.webui.json.DisplayMetadata;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.AddonI18nUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

public class DisplayItemMetadataUtils {
	private static final String defaultFields = "dc.title, dc.title.alternative, dc.contributor.*, dc.subject, dc.date.issued(date), dc.publisher, dc.identifier.citation, dc.relation.ispartofseries, dc.description.abstract, dc.description, dc.identifier.govdoc, dc.identifier.uri(link), dc.identifier.isbn, dc.identifier.issn, dc.identifier.ismn, dc.identifier";

	public static List<DisplayMetadata> getDisplayMetadata(Context context, HttpServletRequest req, Item item,
			String mdString) {
		ItemService itemService = ContentServiceFactory.getInstance().getItemService();
		List<DisplayMetadata> result = new ArrayList<DisplayMetadata>();
		
		String configLine = ConfigurationManager.getProperty("webui.itemdisplay." + mdString);
		if (configLine == null || StringUtils.isEmpty(configLine)) {
			configLine = ConfigurationManager.getProperty("webui.itemdisplay.default");
		}
		if (configLine == null || StringUtils.isEmpty(configLine)) {
			configLine = defaultFields;
		}
		
		for (String f : configLine.split(",")) {
			boolean isDate = false;
			if (f.contains("(date)")) {
				isDate = true;
			}
			f = f.replaceFirst("\\([A-z]*\\)", "");
			List<MetadataValue> mvs = itemService.getMetadataByMetadataString(item, f);
			for (MetadataValue mv : mvs) {
				DisplayMetadata dm = new DisplayMetadata();
				dm.label = AddonI18nUtil.getMessage("metadata."+ f, context);
				dm.value = isDate?new DCDate(mv.getValue()).toString():mv.getValue();
				result.add(dm);
			}
		}
		return result;
	}

}
