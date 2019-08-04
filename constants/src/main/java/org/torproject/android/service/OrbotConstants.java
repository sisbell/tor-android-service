/* Copyright (c) 2009, Nathan Freitas, Orbot/The Guardian Project - http://openideals
.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.service;

public interface OrbotConstants {
    String TAG = "Orbot";//recreate in service
    String PREF_DISABLE_NETWORK = "pref_disable_network";//need this local
    String PREF_TOR_SHARED_PREFS = "org.torproject.android_preferences";//need this local
    String SOCKS_PROXY_PORT = "VPN_SOCKS_PROXY_PORT";//Match one in VpnPrefs
}
