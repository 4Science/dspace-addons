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

import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.Context;

public class JSPUIAddonAdapter implements AddonUIAdapter {
	@Override
	public Context obtainContext(HttpServletRequest request) throws SQLException {
		return UIUtil.obtainContext(request);
	}
}