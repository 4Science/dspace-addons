package org.dspace.app.webui.viewer;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.dspace.content.Bitstream;
import org.dspace.core.Context;

public interface JSPViewer {

	boolean isEmbedded();

	String getViewJSP();

	void prepareViewAttribute(Context context, HttpServletRequest request, Bitstream bitstream) throws SQLException;

}
