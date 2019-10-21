package org.torproject.android.service;

import android.content.Context;
import android.util.Log;
import com.msopentech.thali.toronionproxy.TorInstaller;

import java.io.*;
import java.util.concurrent.TimeoutException;

public class CustomTorInstaller extends TorInstaller {

    private final Context context;
    private final File torrcFile;
    private final File configDir;

    public CustomTorInstaller(Context context, File configDir, File torrcFile) {
        this.context = context;
        this.torrcFile = torrcFile;
        this.configDir = configDir;
    }

    @Override
    public void setup() throws IOException {
        copy(context.getAssets().open("common/geoip"), new File(configDir, "geoip"));
        copy(context.getAssets().open("common/geoip6"), new File(configDir, "geoip6"));
        copy(context.getAssets().open("common/torrc"), new File(configDir, "torrc"));
    }

    @Override
    public void updateTorConfigCustom(String content) throws IOException, TimeoutException {
        updateTorConfigCustom(torrcFile, content);
    }

    /**
     * Opens predefined bridges list as <code>InputStream</code>.
     */
    @Override
    public InputStream openBridgesStream() throws IOException {
        return context.getResources().getAssets().open("common/bridges.txt");
    }

    private static void copy(InputStream is, File target) throws IOException {
        FileOutputStream os = new FileOutputStream(target);
        byte[] buffer = new byte[8192];
        int length = is.read(buffer);
        while (length > 0) {
            os.write(buffer, 0, length);
            length = is.read(buffer);
        }
        os.close();
    }

    private static boolean updateTorConfigCustom(File fileTorRcCustom, String content) throws IOException {
        if (fileTorRcCustom.exists()) {
            fileTorRcCustom.delete();
            Log.d("torResources", "deleting existing torrc.custom");
        } else {
            fileTorRcCustom.createNewFile();
        }

        FileOutputStream fos = new FileOutputStream(fileTorRcCustom, false);
        PrintStream ps = new PrintStream(fos);
        ps.print(content);
        ps.close();
        return true;
    }
}
