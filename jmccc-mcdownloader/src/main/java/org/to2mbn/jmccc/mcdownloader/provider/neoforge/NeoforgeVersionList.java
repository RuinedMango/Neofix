package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import org.to2mbn.jmccc.internal.org.json.JSONArray;
import org.to2mbn.jmccc.internal.org.json.JSONObject;

import java.io.Serializable;
import java.util.*;

public class NeoforgeVersionList implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Map<String, List<NeoforgeVersion>> versions;
    private final Map<String, NeoforgeVersion> NeoforgeVersionMapping;

    public NeoforgeVersionList(
            Map<String, List<NeoforgeVersion>> versions, Map<String, NeoforgeVersion> NeoforgeVersionMapping) {
        Objects.requireNonNull(versions);
        Objects.requireNonNull(NeoforgeVersionMapping);
        this.versions = versions;
        this.NeoforgeVersionMapping = NeoforgeVersionMapping;
    }

    public static NeoforgeVersionList fromJson(JSONObject metaJson) {
        Map<String, List<NeoforgeVersion>> versions = new TreeMap<>();
        Map<String, NeoforgeVersion> NeoforgeVersionMapping = new TreeMap<>();
        System.out.print(metaJson.toString());

        for (String mcVersion : metaJson.keySet()) {
            JSONArray versionJson = metaJson.getJSONArray(mcVersion);
            for (Object NeoforgeVersionObj : versionJson) {
                NeoforgeVersion version = NeoforgeVersion.from((String) NeoforgeVersionObj);
                versions.computeIfAbsent(mcVersion, it -> new ArrayList<>()).add(version);
                NeoforgeVersionMapping.put(version.getNeoforgeVersion(), version);
            }

        }
        return new NeoforgeVersionList(Collections.unmodifiableMap(versions),
                Collections.unmodifiableMap(NeoforgeVersionMapping));
    }

    /**
     * Gets all the forge versions of given minecraft version.
     *
     * @return all the forge versions
     */
    public List<NeoforgeVersion> getVersions(String mcversion) {
        return versions.get(mcversion);
    }

    public Map<String, NeoforgeVersion> getNeoforgeVersionMapping() {
        return NeoforgeVersionMapping;
    }

    public NeoforgeVersion get(String mcversion, int buildNumber) {
        return versions.get(mcversion).get(buildNumber);
    }

    public NeoforgeVersion get(String NeoforgeVersion) {
        return NeoforgeVersionMapping.get(NeoforgeVersion);
    }

    @Override
    public int hashCode() {
        return versions.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NeoforgeVersionList) {
            NeoforgeVersionList another = (NeoforgeVersionList) obj;
            return Objects.equals(versions, another.versions) &&
                    Objects.equals(NeoforgeVersionMapping, another.NeoforgeVersionMapping);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("NeoforgeVersionList [versions=%s, NeoforgeVersionMapping=%s]", versions, NeoforgeVersionMapping);
    }

}
