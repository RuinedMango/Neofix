package org.to2mbn.jmccc.mcdownloader.download.tasks;

import java.io.IOException;
import java.nio.ByteBuffer;

class AppendedDownloadSession<R, S> implements DownloadSession<S> {

	private ResultProcessor<R, S> processor;
	private DownloadSession<R> proxied;

	public AppendedDownloadSession(ResultProcessor<R, S> processor, DownloadSession<R> proxied) {
		this.processor = processor;
		this.proxied = proxied;
	}

	@Override
	public void receiveData(ByteBuffer data) throws IOException {
		proxied.receiveData(data);
	}

	@Override
	public S completed() throws Exception {
		return processor.process(proxied.completed());
	}

	@Override
	public void failed() throws Exception {
		proxied.failed();
	}

}