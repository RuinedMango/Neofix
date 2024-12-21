package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import java.io.Serializable;
import java.util.Objects;

public class NeoforgeVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String minecraftVersion;
    private final String neoforgeVersion;
    private final int buildNumber;
    private final String branch;

    public NeoforgeVersion(String minecraftVersion, String neoforgeVersion, int buildNumber, String branch) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion);
        this.neoforgeVersion = Objects.requireNonNull(neoforgeVersion);
        this.buildNumber = buildNumber;
        this.branch = branch;
    }

    public static NeoforgeVersion from(String fullVersion) {
        String[] split = fullVersion.split("-", 3);
        String mcVersion = split[0];
        String neoforgeVersion = split[1];
        String[] split1 = neoforgeVersion.split("\\.");
        int buildNumber = Integer.parseInt(split1[split1.length - 1]);
        String branch = split.length == 3 ? split[2] : null;
        return new NeoforgeVersion(mcVersion, neoforgeVersion, buildNumber, branch);
    }

    // Getters
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String getNeoforgeVersion() {
        return neoforgeVersion;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public String getBranch() {
        return branch;
    }
    // @formatter:on

    public String getVersionName() {
        return minecraftVersion + "-neoforge-" + neoforgeVersion;
    }

    public String getMavenVersion() {
        String ver = neoforgeVersion;
        if (branch != null)
            ver += "-" + branch;
        return ver;
    }

    @Override
    public String toString() {
        return getVersionName();
    }

    @Override
    public int hashCode() {
        return neoforgeVersion.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NeoforgeVersion) {
            NeoforgeVersion another = (NeoforgeVersion) obj;
            return Objects.equals(minecraftVersion, another.minecraftVersion)
                    && Objects.equals(neoforgeVersion, another.neoforgeVersion)
                    && buildNumber == another.buildNumber
                    && Objects.equals(branch, another.branch);
        }
        return false;
    }

}
