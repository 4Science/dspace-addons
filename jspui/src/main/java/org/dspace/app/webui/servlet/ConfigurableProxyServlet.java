/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.dspace.app.webui.util.IProxyServiceSecurityCheck;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

public class ConfigurableProxyServlet extends ProxyServlet {

	private String moduleName;
	private String serverUrlPropertyName;
	private IProxyServiceSecurityCheck proxyServiceSecurityCheck;
	private boolean forceRewriteRelativePath = false;

	@Override
	public void init() throws ServletException {
		moduleName = getConfigParam("moduleName");
		serverUrlPropertyName = getConfigParam("serverUrlPropertyName");
		forceRewriteRelativePath = "true".equalsIgnoreCase(getConfigParam("forceRewriteRelativePath"));
		String proxyServiceSecurityCheckBeanID = getConfigParam("proxyServiceSecurityCheck");
		if (proxyServiceSecurityCheckBeanID != null) {
			proxyServiceSecurityCheck = new DSpace().getServiceManager()
					.getServiceByName(proxyServiceSecurityCheckBeanID, IProxyServiceSecurityCheck.class);
		} else {
			proxyServiceSecurityCheck = new IProxyServiceSecurityCheck() {
				@Override
				public void extraSecurityCheck(Context context, Bitstream bit, HttpServletRequest req)
						throws AuthorizeException, SQLException, IOException {
					// NO OP
				}
			};
		}
		super.init();
	}

	@Override
	protected void initTarget() throws ServletException {
		targetUri = ConfigurationManager.getProperty(moduleName, serverUrlPropertyName);
		try {
			targetUriObj = new URI(targetUri);
		} catch (Exception e) {
			throw new ServletException("Trying to process targetUri init parameter: " + e, e);
		}
		targetHost = URIUtils.extractHost(targetUriObj);
	}

	protected void copyResponseHeader(HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, Header header) {
		if (!"Content-Length".equals(header.getName())) {
			super.copyResponseHeader(servletRequest,
		            servletResponse, header);
		}
		// skip content-length
	}
	
	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		ProxyServletRequestWrapper proxyServiceRequestWrapper = new ProxyServletRequestWrapper(servletRequest, proxyServiceSecurityCheck);
        Class[] proxyInterfaces = new Class[] { HttpServletRequest.class };
        HttpServletRequest proxyRequest = (HttpServletRequest) Proxy.newProxyInstance(this.getClass().getClassLoader(),
            proxyInterfaces,
            proxyServiceRequestWrapper);
        super.service(proxyRequest, servletResponse);
		if (servletResponse.getHeader("Access-Control-Allow-Origin") != null) {
			if (doLog) {
				log("CORS Header found");
			}
		} else {
			servletResponse.setHeader("Access-Control-Allow-Origin", "*");
			if (doLog) {
				log("CORS Header added");
			}
		}
	}
	
	protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse,
			HttpRequest proxyRequest, HttpServletRequest servletRequest) throws IOException {
		HttpEntity entity = proxyResponse.getEntity();
		if (entity != null) {
			if (entity.getContentType() != null && entity.getContentType().getValue().contains("json")) {
				String responseString = EntityUtils.toString(entity, "UTF-8");
				String rewriteResponseStr = responseString
						.replace(targetUri,
								ConfigurationManager.getProperty("dspace.url") + servletRequest
										.getAttribute("ProxyServletRequestWrapper-requestPath"));
				
				if (forceRewriteRelativePath) {
					String relativeUri = proxyRequest.getRequestLine().getUri().substring(targetUri.length());
					int idxLastSlash = relativeUri.lastIndexOf("/");
					if (idxLastSlash > 0) {
						String relativePath = relativeUri.substring(0, idxLastSlash);
						rewriteResponseStr = rewriteResponseStr.replace("\"" + relativePath,
								"\"" + ConfigurationManager.getProperty("dspace.url")
										+ servletRequest.getAttribute("ProxyServletRequestWrapper-requestPath") + relativePath);
					}
				}
				byte[] rewriteBytes = rewriteResponseStr
						.getBytes();
				servletResponse
						.getOutputStream().write(
								rewriteBytes);
				servletResponse.setContentLength(rewriteBytes.length);
			}
			else {
				Header lenghtHeader = proxyResponse.getFirstHeader("Context-Length");
				if (lenghtHeader != null && lenghtHeader.getValue() != null) {
					servletResponse.addHeader(lenghtHeader.getName(), lenghtHeader.getValue());
				}
				OutputStream servletOutputStream = servletResponse.getOutputStream();
				entity.writeTo(servletOutputStream);
			}
		}
	}
	
    @Override
    protected HttpClient createHttpClient(HttpParams hcParams)
    {
        if(ConfigurationManager.getBooleanProperty("ckan", "use.default.httpclient", false)) {
            return super.createHttpClient(hcParams);
        }
        return new DefaultHttpClient(new ThreadSafeClientConnManager(), hcParams);
    }
}
