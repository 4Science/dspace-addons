package org.dspace.app.webui.servlet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

public class IIIFImageProxyServlet extends ConfigurableProxyServlet {
	
	Logger log = Logger.getLogger(IIIFImageProxyServlet.class);
	
	public IIIFImageProxyServlet () {
		
	}
	
	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {
		try {
	        super.service(servletRequest, servletResponse);
		}
		catch (Throwable ae) {
			Throwable cause = ae.getCause();
			if (cause != null && !(cause instanceof AuthorizeException)) {
				servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	@Override
	protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse,
			HttpRequest proxyRequest, HttpServletRequest servletRequest) throws IOException {
		if ((boolean) servletRequest.getAttribute("require-iiif-auth")) {
    		servletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
		else {
			servletResponse.setStatus(HttpServletResponse.SC_OK);
		}
		
		HttpEntity entity = proxyResponse.getEntity();
		if (entity != null) {
			if (entity.getContentType() != null && entity.getContentType().getValue().contains("json")) {

				String responseString = EntityUtils.toString(entity, "UTF-8");
				
			    if (doLog) {
			        log("Content Type found (JSON)");
			    }
			    
				String rewriteResponseStr = responseString
						.replace(targetUri,
								ConfigurationManager.getProperty("dspace.url") + servletRequest
										.getAttribute("ProxyServletRequestWrapper-requestPath"));
                if (doLog) {
                    log("responseString="+responseString);
                    log("rewriteResponseStr="+rewriteResponseStr);
                }				
                if (doLog) {
                    log("DO force rewrite relative path");
                }
				String relativeUri = proxyRequest.getRequestLine().getUri().substring(targetUri.length());
				int idxLastSlash = relativeUri.lastIndexOf("/");
				if (idxLastSlash > 0) {
				    if (doLog) {
				        log("idxLastSlash="+idxLastSlash);
				    }
					String relativePath = relativeUri.substring(0, idxLastSlash);
					rewriteResponseStr = rewriteResponseStr.replace("\"" + relativePath,
							"\"" + ConfigurationManager.getProperty("dspace.url")
									+ servletRequest.getAttribute("ProxyServletRequestWrapper-requestPath") + relativePath);
				}
				
				Item item = (Item) servletRequest.getAttribute("item");
				rewriteResponseStr = addIIIFAuthenticationService(proxyResponse, servletResponse, proxyRequest, servletRequest, item, rewriteResponseStr);
				
                if (doLog) {
                    log("relativeUri="+relativeUri);
                    log("rewriteResponseStr="+rewriteResponseStr);
                }
				byte[] rewriteBytes = rewriteResponseStr
						.getBytes();
				servletResponse
						.getOutputStream().write(
								rewriteBytes);
				servletResponse.setContentLength(rewriteBytes.length);
			}
			else {
                if (doLog) {
                    log("No JSON content type found!");
                }
				Header lenghtHeader = proxyResponse.getFirstHeader("Context-Length");
				if (lenghtHeader != null && lenghtHeader.getValue() != null) {
					servletResponse.addHeader(lenghtHeader.getName(), lenghtHeader.getValue());
				}
				OutputStream servletOutputStream = servletResponse.getOutputStream();
				entity.writeTo(servletOutputStream);
			}
		}
	}
	
	private String addIIIFAuthenticationService(HttpResponse proxyResponse, HttpServletResponse servletResponse,
			HttpRequest proxyRequest, HttpServletRequest servletRequest, Item item, String rewriteResponseStr) {
		if (StringUtils.endsWith(servletRequest.getPathInfo(), "/info.json")) {
			if (IIIFAuthenticationUtils.clickthroughTermOfUsage(item) != null) {
				rewriteResponseStr = addClickthroughPattern(proxyResponse, servletResponse, proxyRequest, servletRequest, item, rewriteResponseStr);
			}
			else if (StringUtils.equals((String) servletRequest.getAttribute("iiifauth-pattern"), "login")) {
				rewriteResponseStr = addLoginPattern(proxyResponse, servletResponse, proxyRequest, servletRequest, item, rewriteResponseStr);
			}
		}
		return rewriteResponseStr;
	}
	
	private String addClickthroughPattern(HttpResponse proxyResponse, HttpServletResponse servletResponse,
			HttpRequest proxyRequest, HttpServletRequest servletRequest, Item item, String rewriteResponseStr) {
		String clickthrough = ConfigurationManager.getProperty("iiif", "clickthrough");
		String clickthrough_file = ConfigurationManager.getProperty("iiif", "clickthrough.file");
		String clickthrough_metadata = ConfigurationManager.getProperty("iiif", "clickthrough.metadata");
		String clickthrough_metadata_value = null;
		if (item.getMetadata(clickthrough_metadata) != null) {
			clickthrough_metadata_value = item.getMetadata(clickthrough_metadata);
		}
		rewriteResponseStr = rewriteResponseStr.substring(0, StringUtils.lastIndexOf(rewriteResponseStr, "}"));
		
		String clickthrough_default_terms = null;
		try {
			FileInputStream inputStream = new FileInputStream(clickthrough_file);
			clickthrough_default_terms = IOUtils.toString(inputStream);
			inputStream.close();
		} catch (IOException e) {
			log.warn(e.getMessage());
		}
		boolean use_default_terms = false;
		if (StringUtils.equals(clickthrough, "all")) {
			use_default_terms = true;
		}
		else if (StringUtils.equals(clickthrough, "item")) {
			if (StringUtils.isBlank(clickthrough_metadata_value)) {
				use_default_terms = true;
			}
		}
		String context = "http://iiif.io/api/auth/1/context.json";
		String id = "http://scognamiglio.local.it:8080/jspui/json/iiif-cookie?handle=" + item.getHandle();
		String profile = "http://iiif.io/api/auth/1/clickthrough";
		String label = "Restricted material";
		String header = "Terms of usage";
		String description = null;
		if (use_default_terms) {
			description = clickthrough_default_terms.replace("\n", "").replace("\r", "");
		}
		else {
			description = clickthrough_metadata_value;
		}
		String confirmLabel = "I Agree";
		String failureHeader = "Terms of use not accepted!";
		String failureDescription= "You must accept the terms of use to see the content.";
		String tokenId = "http://scognamiglio.local.it:8080/jspui/json/token";
		String tokenProfile = "http://iiif.io/api/auth/1/token";
		return writePattern(rewriteResponseStr, context, id, profile, label, header, description,
				confirmLabel, failureHeader, failureDescription, tokenId, tokenProfile);
	}
	
	private String addLoginPattern(HttpResponse proxyResponse, HttpServletResponse servletResponse,
			HttpRequest proxyRequest, HttpServletRequest servletRequest, Item item, String rewriteResponseStr) {
		rewriteResponseStr = rewriteResponseStr.substring(0, StringUtils.lastIndexOf(rewriteResponseStr, "}"));
		String context = "http://iiif.io/api/auth/1/context.json";
		String id = "http://scognamiglio.local.it:8080/jspui/json/iiif-cookie?login=true&handle=" + item.getHandle(); // change it with .../json/login
		String profile = "http://iiif.io/api/auth/1/login";
		String label = "Login to DSpace";
		String header = "Please Log In";
		String description = "You must log in to view the resource.";
		String confirmLabel = "Login";
		String failureHeader = "Log In failed!";
		String failureDescription= "You must login to see the content.";
		String tokenId = "http://scognamiglio.local.it:8080/jspui/json/token";
		String tokenProfile = "http://iiif.io/api/auth/1/token";
		return writePattern(rewriteResponseStr, context, id, profile, label, header, description,
				confirmLabel, failureHeader, failureDescription, tokenId, tokenProfile);
	}
	
	private String writePattern(String rewriteResponseStr, String context, String id, String profile,
			String label, String header, String description, String confirmLabel, String failureHeader,
			String failureDescription, String tokenId, String tokenProfile) {
		rewriteResponseStr = rewriteResponseStr + ",\r\n\"service\" : {\r\n" + 
				"    \"@context\": \"" + context + "\",\r\n" + 
				"    \"@id\": \"" + id + "\",\r\n" + 
				"    \"profile\": \"" + profile + "\",\r\n" + 
				"    \"label\": \"" + label + "\",\r\n" + 
				"    \"header\": \"" + header + "\",\r\n" + 
				"    \"description\": \"" + description + "\",\r\n" + 
				"    \"confirmLabel\": \"" + confirmLabel + "\",\r\n" + 
				"    \"failureHeader\": \"" + failureHeader + "\",\r\n" + 
				"    \"failureDescription\": \"" + failureDescription + "\",\r\n" +
				"    \"service\": {\r\n" + 
				"        \"@context\": \"" + context + "\",\r\n" + 
				"        \"@id\": \"" + tokenId + "\",\r\n" + 
				"        \"profile\": \"" + tokenProfile + "\"\r\n" + 
				"    }\r\n" + 
				"  }\r\n" + 
				"}\r\n" + 
				"";
		return rewriteResponseStr;
	}
}
