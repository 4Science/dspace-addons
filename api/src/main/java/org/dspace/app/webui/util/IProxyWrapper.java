package org.dspace.app.webui.util;

public interface IProxyWrapper extends java.lang.reflect.InvocationHandler
{
    Object getRealObject();
}
