package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import java.io.File;

import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.FileDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.MemoryDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.AbstractMinecraftDownloadProvider;
import org.to2mbn.jmccc.mcdownloader.provider.ExtendedDownloadProvider;
import org.to2mbn.jmccc.mcdownloader.provider.MinecraftDownloadProvider;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.version.Library;

public class NeoforgeDownloadProvider extends AbstractMinecraftDownloadProvider implements ExtendedDownloadProvider {
    public static final String NEOFORGE_GROUP_ID = "released.net.neoforged";
    public static final String NEOFORGE_ARTIFACT_ID = "neoforge";
	
	private final NeoforgeDownloadSource source;
	
	private MinecraftDownloadProvider upstreamProvider;
	
	public NeoforgeDownloadProvider() {
		this(new DefaultNeoforgeDownloadSource());
	}
	
	public NeoforgeDownloadProvider(NeoforgeDownloadSource source) {
        if (source == null) {
            source = new DefaultNeoforgeDownloadSource();
        }
        this.source = source;
	}
	
    @Override
    public CombinedDownloadTask<Void> library(final MinecraftDirectory mcdir, final Library library) {
        if (NEOFORGE_GROUP_ID.equals(library.getGroupId())) {
            if (NEOFORGE_ARTIFACT_ID.equals(library.getArtifactId())) {
                return universalTask(library.getVersion(), mcdir.getLibrary(library));
            }
        }
        return null;
    }

	
	@Override
	public void setUpstreamProvider(MinecraftDownloadProvider upstreamProvider) {
		this.upstreamProvider = upstreamProvider;
	}
	
    protected CombinedDownloadTask<byte[]> installerTask(String m2Version) {
        Library lib = new Library(NEOFORGE_GROUP_ID, NEOFORGE_ARTIFACT_ID, m2Version, "installer", "jar");
        return CombinedDownloadTask.single(
                new MemoryDownloadTask(source.getNeoforgeMavenRepositoryUrl() + lib.getPath())
                        .cacheable()
                        .cachePool(CacheNames.NEOFORGE_INSTALLER));
    }
	
    protected CombinedDownloadTask<Void> universalTask(String m2Version, File target) {
        String[] types = UNIVERSAL_TYPES;

        @SuppressWarnings("unchecked")
        CombinedDownloadTask<Void>[] tasks = new CombinedDownloadTask[types.length + 1];
        tasks[0] = installerTask(m2Version)
                .andThen(new UniversalDecompressor(target, m2Version));

        for (int i = 0; i < types.length; i++) {
            Library lib = new Library(NEOFORGE_GROUP_ID, NEOFORGE_ARTIFACT_ID, m2Version, "universal", types[i]);
            tasks[i + 1] = CombinedDownloadTask.single(
                    new FileDownloadTask(source.getNeoforgeMavenRepositoryUrl() + lib.getPath(), target)
                            .cachePool(CacheNames.FORGE_UNIVERSAL));
        }

        return CombinedDownloadTask.any(tasks);
    }


}
