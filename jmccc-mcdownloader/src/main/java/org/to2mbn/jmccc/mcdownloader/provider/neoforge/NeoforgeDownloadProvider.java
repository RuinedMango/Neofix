package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.to2mbn.jmccc.internal.org.json.JSONException;
import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.MemoryDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.AbstractMinecraftDownloadProvider;
import org.to2mbn.jmccc.mcdownloader.provider.ExtendedDownloadProvider;
import org.to2mbn.jmccc.mcdownloader.provider.Xml2JsonDecoder;
import org.to2mbn.jmccc.mcdownloader.provider.MinecraftDownloadProvider;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.FileUtils;
import org.to2mbn.jmccc.util.IOUtils;
import org.to2mbn.jmccc.version.Library;
import org.to2mbn.jmccc.version.Version;
import org.to2mbn.jmccc.version.parsing.Versions;

public class NeoforgeDownloadProvider extends AbstractMinecraftDownloadProvider implements ExtendedDownloadProvider {
	public static final String NEOFORGE_GROUP_ID = "releases.net.neoforged";
    public static final String NEOFORGE_ARTIFACT_ID = "neoforge";
    public static final String CLASSIFIER_INSTALLER = "installer";
    public static final String MINECRAFT_MAINCLASS = "net.minecraft.client.Minecraft";

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

    public CombinedDownloadTask<NeoforgeVersionList> neoforgeVersionList() {
        return CombinedDownloadTask.all(
                new MemoryDownloadTask(source.getNeoforgeMetadataUrl())
                        .andThen(new Xml2JsonDecoder())
                        .cacheable()
                        .cachePool(CacheNames.NEOFORGE_VERSION_META)
        ).andThen(it -> NeoforgeVersionList.fromJson((JSONObject) it[0]));
    }

    @Override
    public CombinedDownloadTask<String> gameVersionJson(final MinecraftDirectory mcdir, String version) {
		return null;
    }

    @Override
    public CombinedDownloadTask<Void> library(final MinecraftDirectory mcdir, final Library library) {
        if (NEOFORGE_GROUP_ID.equals(library.getGroupId())) {
            if (NEOFORGE_ARTIFACT_ID.equals(library.getArtifactId())) {
                return universalTask(library.getVersion(), mcdir);

            }
        }
        return null;
    }

    @Override
    public CombinedDownloadTask<Void> gameJar(final MinecraftDirectory mcdir, final Version version) {
        final ResolvedNeoforgeVersion forgeInfo = ResolvedNeoforgeVersion.resolve(version.getRoot());
        if (forgeInfo == null) {
            return null;
        }

        // downloads the super version
        CombinedDownloadTask<Version> baseTask;
        if (forgeInfo.getMinecraftVersion() == null) {
            baseTask = neoforgeVersion(forgeInfo.getNeoforgeVersion())
                    .andThenDownload(forge -> downloadSuperVersion(mcdir, forge.getMinecraftVersion()));
        } else {
            baseTask = downloadSuperVersion(mcdir, forgeInfo.getMinecraftVersion());
        }

        final File targetJar = mcdir.getVersionJar(version);
        
        // copy its superversion's jar
        // remove META-INF
        return baseTask.andThen(superVersion -> {
            purgeMetaInf(mcdir.getVersionJar(superVersion), targetJar);
            return null;
        });
    }

    @Override
    public void setUpstreamProvider(MinecraftDownloadProvider upstreamProvider) {
        this.upstreamProvider = upstreamProvider;
    }

    protected CombinedDownloadTask<byte[]> installerTask(String m2Version) {
        Library lib = new Library(NEOFORGE_GROUP_ID, NEOFORGE_ARTIFACT_ID, m2Version, CLASSIFIER_INSTALLER, "jar");
        return CombinedDownloadTask.single(
                new MemoryDownloadTask(source.getNeoforgeMavenRepositoryUrl() + lib.getPath())
                        .cacheable()
                        .cachePool(CacheNames.FORGE_INSTALLER));
    }

    protected CombinedDownloadTask<Void> universalTask(String m2Version, MinecraftDirectory mcdir) {

        @SuppressWarnings("unchecked")
        CombinedDownloadTask<Void>[] tasks = new CombinedDownloadTask[1];
        tasks[0] = installerTask(m2Version)
                .andThen(new InstallProfileProcessor(mcdir));

        return CombinedDownloadTask.any(tasks);
    }

    protected JSONObject createForgeVersionJson(MinecraftDirectory mcdir, NeoforgeVersion forgeVersion) throws IOException, JSONException {
        JSONObject versionjson = IOUtils.toJson(mcdir.getVersionJson(forgeVersion.getMinecraftVersion()));

        versionjson.remove("downloads");
        versionjson.remove("assets");
        versionjson.remove("assetIndex");
        versionjson.put("id", forgeVersion.getVersionName());
        versionjson.put("mainClass", MINECRAFT_MAINCLASS);
        return versionjson;
    }

    protected void purgeMetaInf(File src, File target) throws IOException {
        FileUtils.prepareWrite(target);
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(src.toPath()));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target.toPath()))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            int read;
            while ((entry = in.getNextEntry()) != null) {
                if (isNotMetaInfEntry(entry)) {
                    out.putNextEntry(entry);
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                    }
                    out.closeEntry();
                }
                in.closeEntry();
            }
        }
    }

    private CombinedDownloadTask<NeoforgeVersion> neoforgeVersion(final String forgeVersion) {
        return neoforgeVersionList()
                .andThen(versionList -> {
                    NeoforgeVersion forge = versionList.get(forgeVersion);
                    if (forge == null) {
                        throw new IllegalArgumentException("Neoforge version not found: " + forgeVersion);
                    }
                    return forge;
                });
    }

    private boolean isNotMetaInfEntry(ZipEntry entry) {
        return !entry.getName().startsWith("META-INF/");
    }

    private CombinedDownloadTask<Version> downloadSuperVersion(final MinecraftDirectory mcdir, String version) {
        return upstreamProvider.gameVersionJson(mcdir, version)
                .andThenDownload(resolvedMcversion -> {
                    final Version superversion = Versions.resolveVersion(mcdir, resolvedMcversion);
                    return upstreamProvider.gameJar(mcdir, superversion).andThenReturn(superversion);
                });
    }

}
