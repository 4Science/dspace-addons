/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

public class ProxyServletInputStream extends ServletInputStream {
    
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
}