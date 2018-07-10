/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.IViewer;
import org.dspace.app.webui.AddonUIAdapterUtil;
import org.dspace.app.webui.services.ViewerConfigurationService;
import org.dspace.app.webui.viewer.JSPViewer;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

public class ExploreServlet extends HttpServlet {
	private static Logger log = Logger.getLogger(ExploreServlet.class);

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Context context = null;
		try {
			context = AddonUIAdapterUtil.obtainContext(request);

			Bitstream bitstream = null;

			// Get the ID from the URL
			UUID bitstreamID = AddonUIAdapterUtil.getUUIDParameter(request, "bitstream_id");
			bitstream = ContentServiceFactory.getInstance().getBitstreamService().find(context, bitstreamID);

			if (bitstream == null) {
				// No bitstream found or filename was wrong -- ID invalid
				log.info(LogManager.getHeader(context, "invalid bitstream id", "ID=" + bitstreamID));
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeAction(context, bitstream, Constants.READ);

			String handle = request.getParameter("handle");
			String viewname = request.getParameter("provider");
			Item i = (Item) HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, handle);
			String title = i.getItemService().getMetadata(i, "dc.title");
			String filename = bitstream.getName();

			BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
			List<MetadataValue> bitstreamMetadataList = bitstreamService.getMetadataByMetadataString(bitstream, IViewer.METADATA_STRING_PROVIDER);
			boolean foundProvider = false;
			for(MetadataValue bitMetadata : bitstreamMetadataList) {
				if(bitMetadata.getValue().equals(viewname)) {
					foundProvider = true;
					break;
				}
			}
			if (!foundProvider) {
				throw new AuthorizeException(LogManager.getHeader(context, "explore",
						"Attempt to access a bitstream with an unregistered view. Bistream ID: " + bitstreamID
								+ " viewprovider: " + viewname));
			}

			log.info(LogManager.getHeader(context, "view_bitstream", "bitstream_id=" + bitstream.getID()));

			new DSpace().getEventService().fireEvent(new UsageEvent(UsageEvent.Action.VIEW, request, context, bitstream));

			request.setAttribute("filename", filename);
			request.setAttribute("handle", handle);
			request.setAttribute("itemTitle", title);

			ViewerConfigurationService viewerConfigurationService = new DSpace()
					.getSingletonService(ViewerConfigurationService.class);
			JSPViewer viewer = viewerConfigurationService.getMapViewers().get(viewname);
			if (viewer != null) {
				viewer.prepareViewAttribute(context, request, bitstream);
				//TODO needed a explore.jsp when there is a JSPUI interface?
				if (!response.isCommitted())
	            {
					request.getRequestDispatcher("/viewers/" + viewer.getViewJSP() + ".jsp").forward(request, response);
	            }
	            else
	            {
	                log.warn("Couldn't show jsp, response is already commited.");
	            }
			} else {
				request.getRequestDispatcher("explore-error.jsp").forward(request, response);
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
	}
}
