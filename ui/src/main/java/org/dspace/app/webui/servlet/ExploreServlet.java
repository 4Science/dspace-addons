/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.util.Locale;

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
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.AddonI18nUtil;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
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
			if (context.getCurrentLocale() == null) {
				context.setCurrentLocale(Locale.ENGLISH);
			}
			else {
				System.out.println(context.getCurrentLocale().toString());
			}

			Bitstream bitstream = null;

			// Get the ID from the URL
			int bitstreamID = AddonUIAdapterUtil.getIntParameter(request, "bitstream_id");
			bitstream = Bitstream.find(context, bitstreamID);

			if (bitstream == null) {
				// No bitstream found or filename was wrong -- ID invalid
				log.info(LogManager.getHeader(context, "invalid bitstream id", "ID=" + bitstreamID));
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			AuthorizeManager.authorizeAction(context, bitstream, Constants.READ);

			String handle = request.getParameter("handle");
			String viewname = request.getParameter("provider");
			Item i = (Item) HandleManager.resolveToObject(context, handle);
			String title = i.getMetadata("dc.title");
			String filename = bitstream.getName();

			Metadatum[] bitstreamMetadataList = bitstream.getMetadataByMetadataString(IViewer.METADATA_STRING_PROVIDER);
			boolean foundProvider = false;
			for(Metadatum bitMetadata : bitstreamMetadataList) {
				if(bitMetadata.value.equals(viewname)) {
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

			request.setAttribute("handle", handle);
			request.setAttribute("itemTitle", title);

			request.setAttribute("headTitle", AddonI18nUtil.getMessage("jsp.head-subtitle", context));
			request.setAttribute("titleMessage", AddonI18nUtil.getMessage("jsp.explore.title", new String[] { handle, filename }, context));
			request.setAttribute("backMessage", AddonI18nUtil.getMessage("jsp.explore.back", new String[] { title }, context));

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
