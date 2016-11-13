/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.hadoop.security.authorize.AuthorizationException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

public interface IProxyServiceSecurityCheck {
	/**
	 * Override this method to perform additional check on the request other
	 * than check that was originated from a readable bitstream
	 * 
	 * @param context
	 * @param bit
	 * @param req
	 */
	public void extraSecurityCheck(Context context, Bitstream bit, HttpServletRequest req)
			throws AuthorizationException, SQLException, IOException;
}
