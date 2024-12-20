package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

public class DefaultNeoforgeDownloadSource implements NeoforgeDownloadSource {
	@Override
	public String getNeoforgeMetadataUrl() {
		return "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
	}

	@Override
	public String getNeoforgeMavenRepositoryUrl() {
		return "https://maven.neoforged.net/";
	}

}
