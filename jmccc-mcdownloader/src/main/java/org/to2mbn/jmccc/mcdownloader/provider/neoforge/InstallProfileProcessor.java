package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.mcdownloader.download.tasks.ResultProcessor;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class InstallProfileProcessor implements ResultProcessor<byte[], Void> {

    private MinecraftDirectory mcdir;

    public InstallProfileProcessor(MinecraftDirectory mcdir) {
        this.mcdir = mcdir;
    }

    @Override
    public Void process(byte[] arg) throws Exception {
        Path tweakedInstaller = mcdir.get("neoforge-installer.jar");
        ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tweakedInstaller));
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(arg))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                //Remove signature in new jar
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(entry.getName()));
                if ("install_profile.json".equals(entry.getName())) {
                    byte[] bytes = IOUtils.toByteArray(in);
                    zos.write(bytes);
                } else if ("net/minecraftforge/installer/SimpleInstaller.class".equals(entry.getName())) {
                    byte[] out = NeoforgeInstallerTweaker.tweakSimpleInstaller(in);
                    zos.write(out);
                } else {
                    zos.write(IOUtils.toByteArray(in));
                }
                in.closeEntry();
                zos.closeEntry();
            }
        }
        zos.close();

        //1.12.2 2851+
        runInstaller(tweakedInstaller);
        Files.delete(tweakedInstaller);
		return null;
    }

    protected JSONObject processJson(JSONObject installprofile) {
        return installprofile.optJSONObject("versionInfo");
    }

    private void runInstaller(Path installerJar) throws Exception {
        //Create default launcher_profiles.json
        Path launcherProfile = mcdir.get("launcher_profiles.json");
        if (!Files.exists(launcherProfile)) {
            Files.write(launcherProfile, "{}".getBytes(StandardCharsets.UTF_8));
        }

        //Run neoforge installer
        try (URLClassLoader cl = new URLClassLoader(new URL[]{installerJar.toFile().toURI().toURL()})) {
            Class<?> installer = cl.loadClass("net.minecraftforge.installer.SimpleInstaller");
            Method main = installer.getMethod("main", String[].class);
            //Installs client
            main.invoke(null, (Object) new String[]{"--installClient", mcdir.getAbsolutePath()});
            
        }
    }
}
