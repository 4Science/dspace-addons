package org.dspace.app.webui.servlet;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.webui.util.IProxyServiceSecurityCheck;
import org.dspace.app.webui.util.IProxyWrapper;

public interface IProxyWrapperFactory {

	IProxyWrapper getWrapper(HttpServletRequest req, IProxyServiceSecurityCheck security);

}
