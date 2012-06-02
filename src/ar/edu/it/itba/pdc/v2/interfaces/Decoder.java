package ar.edu.it.itba.pdc.v2.interfaces;

import java.io.IOException;

import ar.edu.it.itba.pdc.v2.implementations.HTML;
import ar.edu.it.itba.pdc.v2.implementations.RebuiltHeader;

public interface Decoder {

	public void decode(byte[] bytes, int count);

	public boolean keepReading();

	public int getBufferSize();

	public String getHeader(String header);

	public void applyTransformations(byte[] bytes, int count);

	public void applyFilters();

	public void applyRestrictions(byte[] bytes, int count,
			HTTPHeaders requestHeader);

	public byte[] getRotatedImage() throws IOException;

	public byte[] getTransformed();

	public boolean completeHeaders(byte[] bytes, int count);

	public void reset();

	public void parseHeaders(byte[] data, int count);

	public HTTPHeaders getHeaders();

	public byte[] getExtra(byte[] data, int count);

	public void analize(byte[] bytes, int count);

	public RebuiltHeader rebuildHeaders();
	
	public RebuiltHeader rebuildResponseHeaders();

	public void setConfigurator(Configurator configurator);

	public boolean applyTransformations();

	public boolean isImage();

	public boolean isText();

	public RebuiltHeader generateBlockedHeader(String cause);
	
	public HTML generateBlockedHTML(String cause);

	public RebuiltHeader modifiedContentLength(int contentLength);
	
	public boolean contentExpected();
	
}
