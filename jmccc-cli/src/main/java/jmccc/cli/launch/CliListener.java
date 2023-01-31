package jmccc.cli.launch;

import jmccc.cli.download.CliDownloader;
import org.to2mbn.jmccc.launch.ProcessListener;

public class CliListener implements ProcessListener {

    @Override
    public void onLog(String log) {
        System.out.println(log);
    }

    @Override
    public void onErrorLog(String log) {
        System.err.println(log);
    }

    @Override
    public void onExit(int code) {
        System.out.println("Game exited with " + code);
        CliDownloader.downloader.shutdown();
        System.exit(0);
    }
}