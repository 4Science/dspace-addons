/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.factory.CoreServiceFactory;

public class JSONServlet extends HttpServlet {
	private static Logger log = Logger.getLogger(JSONServlet.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Context context = null;
		try {
			context = ContextUtil.obtainContext(request);

			String pluginName = request.getPathInfo();

			if (pluginName == null) {
				pluginName = "";
			}

			if (pluginName.startsWith("/")) {
				pluginName = pluginName.substring(1);
				pluginName = pluginName.split("/")[0];
			}
			JSONRequest jsonReq = (JSONRequest) CoreServiceFactory.getInstance().getPluginService()
					.getNamedPlugin(JSONRequest.class, pluginName);

			if (jsonReq == null) {
				log.error(LogManager.getHeader(context, "jsonrequest",
						"No plugin found for manage the path " + pluginName));
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			jsonReq.setSubPath(pluginName);
			response.setContentType("application/json; charset=UTF-8");
			jsonReq.doJSONRequest(context, request, response);

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
	}
}
