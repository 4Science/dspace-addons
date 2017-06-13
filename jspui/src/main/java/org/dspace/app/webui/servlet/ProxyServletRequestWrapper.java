/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.IProxyServiceSecurityCheck;
import org.dspace.app.webui.util.IProxyWrapper;
import org.dspace.app.webui.util.ProxyServletInputStream;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class ProxyServletRequestWrapper implements IProxyWrapper {

    /** log4j category */
    protected Logger log = Logger.getLogger(this.getClass());
    
    protected HttpServletRequest realRequest;

    protected InputStream inputStream;
    
    protected String rewritePathInfo;

    protected IProxyServiceSecurityCheck proxyServiceSecurityCheck;

    public ProxyServletRequestWrapper(HttpServletRequest realRequest,
            IProxyServiceSecurityCheck proxyServiceSecurityCheck) {
        this.realRequest = realRequest;
        this.proxyServiceSecurityCheck = proxyServiceSecurityCheck;
        try {
            this.inputStream = new ProxyServletInputStream(new BufferedInputStream(realRequest.getInputStream()));
            inputStream.mark(Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.debug("call invoke");
        try {
            log.debug("methodName="+method.getName());
            if (method.getName().equals("getPathInfo")) {
                if (rewritePathInfo != null) {
                    return rewritePathInfo;
                }
                String[] realPath = method.invoke(realRequest, args).toString().split("/", 3);

                Context context = null;
                try {
                    context = UIUtil.obtainContext(realRequest);
                    realRequest.setAttribute("ProxyServletRequestWrapper-requestPath", realRequest.getServletPath() + "/" + realPath[1]); 
                    Bitstream bit = ContentServiceFactory.getInstance().getBitstreamService().find(context, UIUtil.getUUIDParameter(realRequest,realPath[1]));
                    AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeAction(context, bit, Constants.READ);
                    Class[] proxyInterfaces = new Class[] { HttpServletRequest.class };
                    HttpServletRequest proxyRequest = (HttpServletRequest) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                            proxyInterfaces,
                            this);
                    proxyServiceSecurityCheck.extraSecurityCheck(context, bit, proxyRequest);
                    // remove first part containing the bitstreamID to check
                    // extra security
                    rewritePathInfo = "/"+(realPath.length == 3?realPath[2]:"");
                    log.debug("rewritePathInfo="+rewritePathInfo);
                    return rewritePathInfo;
                } finally {
                    // Abort the context if it's still valid
                    if ((context != null) && context.isValid()) {
                        context.abort();
                    }
                }
            }
            else if (method.getName().equals("getInputStream")) {
                inputStream.reset();
                return inputStream;
            }
            return method.invoke(realRequest, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
    
    @Override
    public Object getRealObject()
    {
        return realRequest;
    }
}
