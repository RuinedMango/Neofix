package org.to2mbn.jmccc.version;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.Platform;

class VersionParser {

	public Version parseVersion(MinecraftDirectory minecraftDir, String name) throws IOException, JSONException {
		Set<Library> libraries = new HashSet<>();

		JSONObject json = readJson(minecraftDir.getVersionJson(name));
		String version = json.getString("id");
		String assets = json.optString("assets", "legacy");
		String mainClass = json.getString("mainClass");
		String launchArgs = json.getString("minecraftArguments");
		String type = json.optString("type", null);
		loadDepends(json.getJSONArray("libraries"), libraries);

		Map<String, DownloadInfo> downloads = json.has("downloads") ? resolveDownloads(json.getJSONObject("downloads")) : null;
		AssetIndexInfo assetIndexDownloadInfo = json.has("assetIndex") ? resolveAssetIndexInfo(json.getJSONObject("assetIndex")) : null;

		String jarPath;

		if (json.has("inheritsFrom")) {
			String inheritsFrom;
			do {
				inheritsFrom = json.getString("inheritsFrom");
				json = readJson(minecraftDir.getVersionJson(inheritsFrom));
				loadDepends(json.getJSONArray("libraries"), libraries);
				assets = json.optString("assets", "legacy");
			} while (json.has("inheritsFrom"));
			jarPath = getVersionJarPath(inheritsFrom);
		} else {
			jarPath = getVersionJarPath(version);
		}

		return new Version(version, type, mainClass, assets, launchArgs, jarPath, Collections.unmodifiableSet(libraries), assets.equals("legacy"), assetIndexDownloadInfo, downloads);
	}

	public Set<Asset> parseAssets(MinecraftDirectory minecraftDir, String name) throws IOException, JSONException {
		JSONObject json = readJson(minecraftDir.getAssetIndex(name));
		JSONObject objects = json.getJSONObject("objects");
		Set<Asset> assets = new HashSet<>();
		for (Object rawVirtualPath : objects.keySet()) {
			String virtualPath = (String) rawVirtualPath;
			JSONObject object = objects.getJSONObject(virtualPath);
			String hash = object.getString("hash");
			int size = object.getInt("size");
			assets.add(new Asset(virtualPath, hash, size));
		}
		return Collections.unmodifiableSet(assets);
	}

	private void loadDepends(JSONArray librariesList, Collection<Library> libraries) {
		for (int i = 0; i < librariesList.length(); i++) {
			Library library = resolveLibrary(librariesList.getJSONObject(i));
			if (library != null) {
				libraries.add(library);
			}
		}
	}

	private boolean isAllowed(JSONArray rules) {
		// by default it's allowed
		if (rules.length() == 0) {
			return true;
		}

		// else it's disallow by default
		boolean allow = false;
		for (int i = 0; i < rules.length(); i++) {
			JSONObject rule = rules.getJSONObject(i);

			boolean action = rule.get("action").equals("allow");

			// apply by default
			boolean apply = true;

			if (rule.has("os")) {
				// don't apply by default if has os rule
				apply = false;

				JSONObject osRule = rule.getJSONObject("os");
				String name = osRule.getString("name");
				String version = osRule.has("version") ? osRule.getString("version") : null;

				if (Platform.CURRENT.name().equalsIgnoreCase(name)) {
					if (version == null || System.getProperty("os.version").matches(version)) {
						apply = true;
					}
				}
			}

			if (apply) {
				allow = action;
			}
		}

		return allow;
	}

	private Library resolveLibrary(JSONObject json) {
		if (json.has("rules") && !isAllowed(json.getJSONArray("rules"))) {
			return null;
		}

		String[] splited = json.getString("name").split(":", 3);
		String domain = splited[0];
		String name = splited[1];
		String version = splited[2];

		String url = json.has("url") ? json.getString("url") : null;
		String[] checksums = json.has("checksums") ? resolveChecksums(json.getJSONArray("checksums")) : null;

		boolean isNative = json.has("natives");
		if (isNative) {
			String natives = resolveNative(json.getJSONObject("natives"));
			Set<String> excludes = json.has("extract") ? resolveExtractExclude(json.getJSONObject("extract")) : null;
			return new Native(domain, name, version, resolveLibraryDownload(json, natives), natives, excludes, url, checksums);
		} else {
			return new Library(domain, name, version, resolveLibraryDownload(json, null), url, checksums);
		}
	}

	private LibraryInfo resolveLibraryDownload(JSONObject json, String natives) {
		JSONObject downloads = json.optJSONObject("downloads");
		if (downloads == null) {
			return null;
		}
		JSONObject artifact;
		if (natives == null) {
			artifact = downloads.optJSONObject("artifact");
		} else {
			JSONObject classifiers = downloads.getJSONObject("classifiers");
			if (classifiers == null) {
				return null;
			}
			artifact = classifiers.optJSONObject(natives);
		}
		if (artifact == null) {
			return null;
		}
		return resolveLibraryInfo(artifact);
	}

	private String resolveNative(JSONObject natives) {
		String archName = Platform.CURRENT.name().toLowerCase();

		if (natives.has(archName)) {
			return natives.getString(archName).replaceAll("\\Q${arch}", Platform.isX64() ? "64" : "32");
		} else {
			return null;
		}
	}

	private Set<String> resolveExtractExclude(JSONObject extract) {
		if (!extract.has("exclude")) {
			return null;
		}

		Set<String> excludes = new HashSet<>();
		JSONArray excludesArray = extract.getJSONArray("exclude");
		for (int i = 0; i < excludesArray.length(); i++) {
			excludes.add(excludesArray.getString(i));
		}
		return Collections.unmodifiableSet(excludes);
	}

	private String getVersionJarPath(String version) {
		return version + "/" + version + ".jar";
	}

	private String[] resolveChecksums(JSONArray sumarray) {
		String[] checksums = new String[sumarray.length()];
		for (int i = 0; i < sumarray.length(); i++) {
			checksums[i] = sumarray.getString(i);
		}
		return checksums;
	}

	private JSONObject readJson(File file) throws IOException, JSONException {
		try (Reader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), "UTF-8")) {
			return new JSONObject(new JSONTokener(reader));
		}
	}

	private DownloadInfo resolveDownloadInfo(JSONObject json) {
		String url = json.optString("url", null);
		String checksum = json.optString("sha1", null);
		long size = json.optLong("size", -1);
		return new DownloadInfo(url, checksum, size);
	}

	private Map<String, DownloadInfo> resolveDownloads(JSONObject json) {
		Map<String, DownloadInfo> downloads = new HashMap<>();
		for (Object rawkey : json.keySet()) {
			String key = (String) rawkey;
			downloads.put(key, resolveDownloadInfo(json.getJSONObject(key)));
		}
		return Collections.unmodifiableMap(downloads);
	}

	private AssetIndexInfo resolveAssetIndexInfo(JSONObject json) {
		DownloadInfo base = resolveDownloadInfo(json);
		String id = json.getString("id");
		long totalSize = json.optLong("totalSize", -1);
		return new AssetIndexInfo(base.getUrl(), base.getChecksum(), base.getSize(), id, totalSize);
	}

	private LibraryInfo resolveLibraryInfo(JSONObject json) {
		DownloadInfo base = resolveDownloadInfo(json);
		String path = json.optString("path", null);
		return new LibraryInfo(base.getUrl(), base.getChecksum(), base.getSize(), path);
	}

}