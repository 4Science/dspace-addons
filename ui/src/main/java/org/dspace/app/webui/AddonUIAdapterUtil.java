/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class AddonUIAdapterUtil {

	public static Context obtainContext(HttpServletRequest realRequest) throws SQLException {
		AddonUIAdapter adapter = new DSpace().getSingletonService(AddonUIAdapter.class);
		return adapter.obtainContext(realRequest);
	}

}
