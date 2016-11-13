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

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.dspace.app.webui.util.IProxyServiceSecurityCheck;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class ProxyServletRequestWrapper implements java.lang.reflect.InvocationHandler {

	private HttpServletRequest realRequest;

	private InputStream inputStream;
	
	private String rewritePathInfo;

	private IProxyServiceSecurityCheck proxyServiceSecurityCheck;

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
		try {
			if (method.getName().equals("getPathInfo")) {
				if (rewritePathInfo != null) {
					return rewritePathInfo;
				}
				String[] realPath = method.invoke(realRequest, args).toString().split("/", 3);

				Context context = null;
				try {
					context = UIUtil.obtainContext(realRequest);
					realRequest.setAttribute("ProxyServletRequestWrapper-requestPath", realRequest.getServletPath() + "/" + realPath[1]); 
					Bitstream bit = Bitstream.find(context, Integer.parseInt(realPath[1]));
					AuthorizeManager.authorizeAction(context, bit, Constants.READ);
					proxyServiceSecurityCheck.extraSecurityCheck(context, bit, realRequest);
					// remove first part containing the bitstreamID to check
					// extra security
					rewritePathInfo = "/"+(realPath.length == 3?realPath[2]:"");
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
	
	private static class ProxyServletInputStream extends ServletInputStream {
		private InputStream is;

		public ProxyServletInputStream(InputStream is) {
			this.is = is;
		}
		
	    public int read() throws IOException {
	    	return is.read();
	    }

	    public int read(byte b[]) throws IOException {
	        return is.read(b, 0, b.length);
	    }

	    public int read(byte b[], int off, int len) throws IOException {
	    	return is.read(b, off, len);
	    }

	    public long skip(long n) throws IOException {
	    	return is.skip(n);
	    }

	    public int available() throws IOException {
	        return is.available();
	    }

	    public void close() throws IOException {
	    	is.close();
	    }

	    public synchronized void mark(int readlimit) {
	    	is.mark(readlimit);
	    }

	    public synchronized void reset() throws IOException {
	        is.reset();
	    }

	    public boolean markSupported() {
	        return is.markSupported();
	    }

        public boolean isFinished()
        {
            try
            {
                return ((is.available())==0);
            }
            catch (IOException e)
            {
                return false;
            }
        }

        public boolean isReady()
        {
            return true;
        }

        public void setReadListener(ReadListener readListener)
        {
            throw new RuntimeException("Not implemented");            
        }
	}
}
