package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ResolvedNeoforgeVersion implements Serializable {

    private static final Pattern NEOFORGE_VERSION_PATTERN_1 = Pattern.compile("^([\\w\\.\\-]+)-[Nn]eoforge\\1?-([\\w\\.\\-]+)$");
    private static final Pattern NEOFORGE_VERSION_PATTERN_2 = Pattern.compile("^([\\w\\.\\-]+)-[Nn]eoforge([\\w\\.\\-]+)$");
    private static final Pattern NEOFORGE_VERSION_PATTERN_3 = Pattern.compile("^Neoforge([\\w\\.\\-]+)$");
    private static final long serialVersionUID = 1L;
    private String neoforgeVersion;
    private String minecraftVersion;
    public ResolvedNeoforgeVersion(NeoforgeVersion version) {
        this(version.getNeoforgeVersion(), version.getMinecraftVersion());
    }

    public ResolvedNeoforgeVersion(String neoforgeVersion, String minecraftVersion) {
        this.neoforgeVersion = neoforgeVersion;
        this.minecraftVersion = minecraftVersion;
    }

    public static ResolvedNeoforgeVersion resolve(String version) {
        Matcher matcher = NEOFORGE_VERSION_PATTERN_1.matcher(version);
        if (matcher.matches()) {
            String neoforgeVersion = matcher.group(2);
            String mcversion = matcher.group(1);
            return new ResolvedNeoforgeVersion(neoforgeVersion, mcversion);
        }

        matcher = NEOFORGE_VERSION_PATTERN_2.matcher(version);
        if (matcher.matches()) {
            String neoforgeVersion = matcher.group(2);
            String mcversion = matcher.group(1);
            return new ResolvedNeoforgeVersion(neoforgeVersion, mcversion);
        }

        matcher = NEOFORGE_VERSION_PATTERN_3.matcher(version);
        if (matcher.matches()) {
            String neoforgeVersion = matcher.group(1);
            return new ResolvedNeoforgeVersion(neoforgeVersion, null);
        }

        return null;
    }

    public String getNeoforgeVersion() {
        return neoforgeVersion;
    }

    /**
     * @return the minecraft version of the forge version, may be null
     */
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String getVersionName() {
        return minecraftVersion + "-" + neoforgeVersion;
    }

    @Override
    public String toString() {
        return getVersionName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(neoforgeVersion, minecraftVersion);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResolvedNeoforgeVersion) {
            ResolvedNeoforgeVersion another = (ResolvedNeoforgeVersion) obj;
            return Objects.equals(neoforgeVersion, another.neoforgeVersion) &&
                    Objects.equals(minecraftVersion, another.minecraftVersion);
        }
        return false;
    }

}
