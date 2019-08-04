package org.torproject.android.service.vpn;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.support.v4.content.LocalBroadcastManager;
import org.torproject.android.service.TorServiceConstants;

/**
 * Created by n8fr8 on 9/26/16.
 */
public class TorVpnService extends VpnService {
    OrbotVpnManager mVpnManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mVpnManager = new OrbotVpnManager(this);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mLocalBroadcastReceiver,
                new IntentFilter(TorServiceConstants.LOCAL_ACTION_PORTS));
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        mVpnManager.handleIntent(new Builder(), intent);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(mLocalBroadcastReceiver);
    }

    /**
     * The state and log info from {TorService} are sent to the UI here in
     * the form of a local broadcast. Regular broadcasts can be sent by any app,
     * so local ones are used here so other apps cannot interfere with Orbot's
     * operation.
     */
    private BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;

            if (action.equals(TorServiceConstants.LOCAL_ACTION_PORTS)) {

                mVpnManager.handleIntent(new Builder(),intent);
            }
        }
    };
}
