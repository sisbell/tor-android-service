/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.torproject.android.service.vpn;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.VpnService;
import android.net.VpnService.Builder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;
import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticatorNone;
import com.torproject.android.service.vpn.R;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;

import static org.torproject.android.service.TorServiceConstants.EXTRA_SOCKS_PROXY_PORT;
import static org.torproject.android.service.vpn.VpnConstants.EXTRA_DNS_PORT;
import static org.torproject.android.service.vpn.VpnConstants.LOCAL_ACTION_PORTS;
import static org.torproject.android.service.vpn.VpnPrefs.SOCKS_PROXY_PORT;
import static org.torproject.android.service.vpn.VpnUtils.getSharedPrefs;
import static org.torproject.android.service.vpn.VpnUtils.killProcess;

public final class OrbotVpnManager implements Handler.Callback {
    private static final String TAG = "OrbotVpnService";
    private final static int VPN_MTU = 1500;
    private final static boolean mIsLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private final SharedPreferences prefs;
    private File filePdnsd;
    private PendingIntent mConfigureIntent;
    private Thread mThreadVPN;
    private String mSessionName = "OrbotVPN";
    private ParcelFileDescriptor mInterface;
    private int mTorSocks = -1;
    private int socksProxyServerPort = -1;
    private int mTorDns = -1;
    private ProxyServer mSocksProxyServer;
    private boolean isRestart = false;
    private VpnService mService;

    OrbotVpnManager(VpnService service) {
        mService = service;
        File nativeDir = new File(service.getApplicationInfo().nativeLibraryDir);
        filePdnsd = new File(nativeDir, "libpdnsd.so");
        filePdnsd.setExecutable(true);
        Tun2Socks.init();
        prefs = getSharedPrefs(mService.getApplicationContext());
    }

    private static File makePdnsdConf(Context context, File fileDir, String torDnsHost, int torDnsPort,
                                      String pdnsdHost, int pdnsdPort) throws IOException {
        String conf = String.format(context.getString(R.string.pdnsd_conf), torDnsHost, torDnsPort,
                fileDir.getCanonicalPath(), pdnsdHost, pdnsdPort);
        Log.d(TAG, "pdsnd conf:" + conf);
        File f = new File(fileDir, "pdnsd.conf");
        if (f.exists()) {
            f.delete();
        }

        FileOutputStream fos = new FileOutputStream(f, false);
        PrintStream ps = new PrintStream(fos);
        ps.print(conf);
        ps.close();

        File cache = new File(fileDir, "pdnsd.cache");
        if (!cache.exists()) {
            try {
                cache.createNewFile();
            } catch (Exception e) {

            }
        }
        return f;
    }

    int handleIntent(Builder builder, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if ("start".equals(action)) {
                // stop the previous session by interrupting the thread.
                if (mThreadVPN != null && mThreadVPN.isAlive())
                    stopVPN();
                if (mTorSocks != -1) {
                    if (!mIsLollipop) {
                        startSocksBypass();
                    }
                    setupTun2Socks(builder);
                }
            } else if ("stop".equals(action)) {
                Log.d(TAG, "stop OrbotVPNService service!");
                stopVPN();
            } else if (LOCAL_ACTION_PORTS.equals(action)) {
                Log.d(TAG, "starting OrbotVPNService service!");
                int newTorSocks = intent.getIntExtra(EXTRA_SOCKS_PROXY_PORT,-1);
                int newTorDns = intent.getIntExtra(EXTRA_DNS_PORT,-1);
                if ((mTorSocks != newTorSocks || mTorDns != newTorDns)) {
                    mTorSocks = newTorSocks;
                    mTorDns = newTorDns;
                    if (!mIsLollipop) {
                        startSocksBypass();
                    }
                }
                setupTun2Socks(builder);
            }
        }
        return Service.START_STICKY;
    }

    private void startSocksBypass() {
        new Thread() {
            public void run() {
                //generate the proxy port that the
                if (socksProxyServerPort == -1) {
                    try {
                        socksProxyServerPort = (int) ((Math.random() * 1000) + 10000);
                        prefs.edit().putInt(SOCKS_PROXY_PORT, socksProxyServerPort).apply();
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to access localhost", e);
                        throw new RuntimeException("Unable to access localhost: " + e);
                    }
                }
                if (mSocksProxyServer != null) {
                    stopSocksBypass();
                }
                try {
                    mSocksProxyServer = new ProxyServer(new ServerAuthenticatorNone(null, null));
                    ProxyServer.setVpnService(mService);
                    mSocksProxyServer.start(socksProxyServerPort, 5, InetAddress.getLocalHost());
                } catch (Exception e) {
                    Log.e(TAG, "error getting host", e);
                }
            }
        }.start();
    }

    private synchronized void stopSocksBypass() {
        if (mSocksProxyServer != null) {
            mSocksProxyServer.stop();
            mSocksProxyServer = null;
        }
    }

    private void stopVPN() {
        if (mIsLollipop)
            stopSocksBypass();

        if (mInterface != null) {
            try {
                Log.d(TAG, "closing interface, destroying VPN interface");
                mInterface.close();
                mInterface = null;
            } catch (Exception e) {
                Log.d(TAG, "error stopping tun2socks", e);
            } catch (Error e) {
                Log.d(TAG, "error stopping tun2socks", e);
            }
        }
        stopDns();
        Tun2Socks.stop();
        mThreadVPN = null;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(mService, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private synchronized void setupTun2Socks(final Builder builder) {
        if (mInterface != null) {
            isRestart = true;
            Tun2Socks.stop();
            stopDns();
        }

        mThreadVPN = new Thread() {
            public void run() {
                try {
                    if (isRestart) {
                        Log.d(TAG, "is a restart... let's wait for a few seconds");
                        Thread.sleep(3000);
                    }

                    final String vpnName = "OrbotVPN";
                    final String localhost = "127.0.0.1";
                    final String virtualGateway = "192.168.200.1";
                    final String virtualIP = "192.168.200.2";
                    final String virtualNetMask = "255.255.255.0";
                    final String dummyDNS = "1.1.1.1"; //this is intercepted by the tun2socks library, but we must put in a valid DNS to start
                    final String defaultRoute = "0.0.0.0";
                    final int pdnsdPort = 8091;
                    final String localSocks = localhost + ':' + mTorSocks;
                    final String localDNS = virtualGateway + ':' + pdnsdPort;
                    final boolean localDnsTransparentProxy = true;


                    builder.setMtu(VPN_MTU);
                    builder.addAddress(virtualGateway, 32);
                    builder.setSession(vpnName);
                    builder.addDnsServer(dummyDNS);
                    builder.addRoute(dummyDNS, 32);
                    //route all traffic through VPN (we might offer country specific exclude lists in the future)
                    builder.addRoute(defaultRoute, 0);

                    if (mIsLollipop)
                        doLollipopAppRouting(builder);

                    // Create a new interface using the builder and save the parameters.
                    ParcelFileDescriptor newInterface = builder.setSession(mSessionName)
                            .setConfigureIntent(mConfigureIntent)
                            .establish();
                    if (mInterface != null) {
                        Log.d(TAG, "Stopping existing VPN interface");
                        mInterface.close();
                        mInterface = null;
                    }
                    mInterface = newInterface;
                    isRestart = false;

                    //start PDNSD daemon pointing to actual DNS
                    startDNS(filePdnsd.getCanonicalPath(), localhost, mTorDns, virtualGateway, pdnsdPort);
                    Tun2Socks.start(mInterface, VPN_MTU, virtualIP, virtualNetMask, localSocks, localDNS, localDnsTransparentProxy);
                } catch (Exception e) {
                    Log.d(TAG, "tun2Socks has stopped", e);
                }
            }
        };
        mThreadVPN.start();
    }

    private boolean stopDns() {
        try {
            killProcess(filePdnsd, "-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        File filePid = new File(mService.getFilesDir(), "pdnsd.pid");
        String pid = null;
        if (filePid.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(filePid));
                pid = reader.readLine().trim();
                killProcess(pid, "-9");
                filePid.delete();
                return true;

            } catch (Exception e) {
                Log.e(TAG, "error killing DNS Process: " + pid, e);
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void doLollipopAppRouting(Builder builder) throws NameNotFoundException {
        ArrayList<TorifiedApp> apps = TorifiedApp.getApps(mService, prefs);
        boolean perAppEnabled = false;
        for (TorifiedApp app : apps) {
            if (app.isTorified() && (!app.getPackageName().equals(mService.getPackageName()))) {
                builder.addAllowedApplication(app.getPackageName());
                perAppEnabled = true;
            }
        }
        if (!perAppEnabled)
            builder.addDisallowedApplication(mService.getPackageName());
    }

    private void startDNS(String pdnsPath, String torDnsHost, int torDnsPort, String pdnsdHost, int pdnsdPort) throws IOException {
        File fileConf = makePdnsdConf(mService, mService.getFilesDir(), torDnsHost, torDnsPort, pdnsdHost, pdnsdPort);
        String[] cmdString = {filePdnsd.getCanonicalPath(), "-c", fileConf.toString()};
        ProcessBuilder pb = new ProcessBuilder(cmdString);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try {
            proc.waitFor();
        } catch (Exception e) {
        }

        Log.i(TAG, "PDNSD: " + proc.exitValue());
        if (proc.exitValue() != 0) {
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                Log.d(TAG, "pdnsd: " + line);
            }
        }
    }
}
