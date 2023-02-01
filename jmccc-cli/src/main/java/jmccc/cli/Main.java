package jmccc.cli;

import jmccc.cli.download.CliDownloader;
import jmccc.cli.download.ModLoaderDownloader;
import jmccc.cli.launch.CliAuthenticator;
import jmccc.cli.launch.CliConfig;
import jmccc.cli.launch.CliLauncher;
import jmccc.microsoft.MicrosoftAuthenticator;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.to2mbn.jmccc.auth.Authenticator;
import org.to2mbn.jmccc.auth.OfflineAuthenticator;
import org.to2mbn.jmccc.option.LaunchOption;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.version.Version;

import java.util.concurrent.ExecutionException;

public class Main {

    /**
     * Arguments:
     * Microsoft Account: [--microsoft] [--clientId client_id]
     * Offline: [--offline player_name]
     * [--bmclapi] [--dir minecraft_directory] [version]
     */
    public static void main(String... args) {
        try {
            internal(args);
        } catch (Exception e) {
            e.printStackTrace();
            CliDownloader.downloader.shutdown();
            System.exit(0);
        }
    }

    private static void internal(String... args) throws Exception {
        //Parse args
        OptionParser parser = new OptionParser();
        parser.accepts("microsoft");
        parser.accepts("bmclapi");
        OptionSpec<String> offlineOption = parser.accepts("offline")
                .availableUnless("microsoft").withOptionalArg().defaultsTo("Player");
        OptionSpec<String> dirOption = parser.accepts("dir").withOptionalArg().defaultsTo(".minecraft");
        OptionSpec<String> clientIdOption = parser.accepts("clientId")
                .availableIf("microsoft").withOptionalArg();
        OptionSpec<String> versionOption = parser.nonOptions();

        OptionSet options = parser.parse(args);
        String player = offlineOption.value(options);
        String dir = dirOption.value(options);
        String version = versionOption.value(options);
        MinecraftDirectory minecraftDirectory = new MinecraftDirectory(dir);
        boolean isMicrosoftAccount = options.has("microsoft");
        boolean isBmclApi = options.has("bmclapi");
        String clientId = clientIdOption.value(options);

        //Init config
        CliConfig.initConfig(minecraftDirectory);
        if (clientId != null) {
            MicrosoftAuthenticator.setClientId(clientId);
        }
        CliDownloader.init(isBmclApi);

        //Check special version
        version = parseVersion(version);
        System.out.println("Version: " + version);

        //Download game if not exist
        if (!minecraftDirectory.getVersionJson(version).exists()) {
            Version v = CliDownloader.download(minecraftDirectory, version);
            version = v.getVersion();
        }

        //Launch game
        Authenticator authenticator = isMicrosoftAccount ?
                CliAuthenticator.getMicrosoftAuthenticator() :
                OfflineAuthenticator.name(player);
        LaunchOption option = new LaunchOption(version, authenticator, minecraftDirectory);
        CliLauncher.launch(option);
    }

    private static String parseVersion(String version) throws ExecutionException, InterruptedException {
        if (version == null) {
            version = "release";
        }
        switch (version) {
            case "latest":
            case "stable":
            case "release":
                return CliDownloader.getLatestRelease();
            case "snapshot":
                return CliDownloader.getLatestSnapshot();
            case "forge":
                return ModLoaderDownloader.getLatestForge().getVersionName();
            case "liteloader":
                return ModLoaderDownloader.getLatestLiteLoader().getVersionName();
            case "fabric":
                return ModLoaderDownloader.getLatestFabric().getVersionName();
            case "fabric-snapshot":
                return ModLoaderDownloader.getLatestFabricSnapshot().getVersionName();
            case "quilt":
                return ModLoaderDownloader.getLatestQuilt().getVersionName();
            case "quilt-snapshot":
                return ModLoaderDownloader.getLatestQuiltSnapshot().getVersionName();
            default:
                return version;
        }
    }
}
