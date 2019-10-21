
package org.torproject.android.service.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.msopentech.thali.toronionproxy.BridgeType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Prefs {

    private final static String PREF_BRIDGES_ENABLED = "pref_bridges_enabled";
    private final static String PREF_BRIDGES_LIST = "pref_bridges_list";
    private final static String PREF_CUSTOM_BRIDGES = "pref_custom_bridges";
    private final static String PREF_BRIDGE_TYPES = "pref_bridge_types";

    private final static String PREF_DEFAULT_LOCALE = "pref_default_locale";
    private final static String PREF_ENABLE_LOGGING = "pref_enable_logging";
    private final static String PREF_EXPANDED_NOTIFICATIONS = "pref_expanded_notifications";
    private final static String PREF_HAS_ROOT = "has_root";
    private final static String PREF_PERSIST_NOTIFICATIONS = "pref_persistent_notifications";
    private final static String PREF_START_ON_BOOT = "pref_start_boot";
    private final static String PREF_ALLOW_BACKGROUND_STARTS = "pref_allow_background_starts";
    private final static String PREF_OPEN_PROXY_ON_ALL_INTERFACES = "pref_open_proxy_on_all_interfaces";
    private final static String PREF_TRANSPARENT = "pref_transparent";
    private final static String PREF_TRANSPARENT_ALL = "pref_transparent_all";
    private final static String PREF_TRANSPARENT_TETHERING = "pref_transparent_tethering";
    private final static String PREF_TRANSPROXY_REFRESH = "pref_transproxy_refresh";
    private final static String PREF_USE_SYSTEM_IPTABLES = "pref_use_sys_iptables";
    private final static String PREF_USE_VPN = "pref_vpn";
    private final static String PREF_EXIT_NODES = "pref_exit_nodes";
    
    private static SharedPreferences prefs;

    public static void setContext(Context context) {
        if (prefs == null)
            prefs = TorServiceUtils.getSharedPrefs(context);
    }

    private static void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    private static void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public static boolean bridgesEnabled() {
        //if phone is in Farsi, enable bridges by default
        boolean bridgesEnabledDefault = Locale.getDefault().getLanguage().equals("fa");
        return prefs.getBoolean(PREF_BRIDGES_ENABLED, bridgesEnabledDefault);
    }

    public static void putBridgesEnabled(boolean value) {
        putBoolean(PREF_BRIDGES_ENABLED, value);
    }

    public static String getBridgesList() {
        String defaultBridgeType = (Locale.getDefault().getLanguage().equals("fa")) ? "meek": "obfs4";
        String list = prefs.getString(PREF_BRIDGES_LIST, defaultBridgeType);
        return (list.trim().isEmpty()) ? defaultBridgeType : list;
    }

    public static void setBridgesList(String value) {
        putString(PREF_BRIDGES_LIST, value);
    }

    /**
     * Set a list of bridge types to be used. These will be used to pull bridges from the bridges.txt file
     */
    public static void setBridgeTypes(List<String> bridgeTypes) {
        if(bridgeTypes == null || bridgeTypes.isEmpty()) {
            putString(PREF_BRIDGE_TYPES, null);
        } else {
            putString(PREF_BRIDGE_TYPES, join(bridgeTypes));
        }
    }

    public static List<String> getBridgeTypes() {
        String bridgeTypes = prefs.getString(PREF_BRIDGE_TYPES, null);
        if(bridgeTypes == null || bridgeTypes.isEmpty()) {
            BridgeType defaultBridgeType = (Locale.getDefault().getLanguage().equals("fa")) ? BridgeType.MEEK_LITE : BridgeType.OBFS4;
            return Collections.singletonList(defaultBridgeType.name().toLowerCase());
        }
        return Arrays.asList(bridgeTypes.split("[|]"));
    }

    /**
     * These entries may be type-specified like:
     *
     * <code>
     *  obfs3 169.229.59.74:31493 AF9F66B7B04F8FF6F32D455F05135250A16543C9
     * </code>
     *
     * Or it may just be a custom entry like
     *
     * <code>
     *    69.163.45.129:443 9F090DE98CA6F67DEEB1F87EFE7C1BFD884E6E2F
     * </code>
     */
    public static void setCustomBridges(List<String> bridges) {
        if(bridges == null || bridges.isEmpty()) {
            putString(PREF_CUSTOM_BRIDGES, null);
        } else {
            putString(PREF_CUSTOM_BRIDGES, join(bridges));
        }
    }

    public static List<String> getCustomBridges() {
        String customBridges =  prefs.getString(PREF_CUSTOM_BRIDGES, null);
        if(customBridges == null || customBridges.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(customBridges.split("[|]"));
    }

    public static String getDefaultLocale() {
        return prefs.getString(PREF_DEFAULT_LOCALE, Locale.getDefault().getLanguage());
    }

    public static void setDefaultLocale(String value) {
        putString(PREF_DEFAULT_LOCALE, value);
    }

    public static boolean useSystemIpTables() {
        return prefs.getBoolean(PREF_USE_SYSTEM_IPTABLES, false);
    }

    public static boolean useRoot() {
        return prefs.getBoolean(PREF_HAS_ROOT, false);
    }

    public static boolean useTransparentProxying() {
        return prefs.getBoolean(PREF_TRANSPARENT, false);
    }

    public static void disableTransparentProxying() {
        putBoolean(PREF_TRANSPARENT, false);
    }

    public static boolean transparentProxyAll() {
        return prefs.getBoolean(PREF_TRANSPARENT_ALL, false);
    }

    public static boolean transparentTethering() {
        return prefs.getBoolean(PREF_TRANSPARENT_TETHERING, false);
    }

    public static boolean transProxyNetworkRefresh() {
        return prefs.getBoolean(PREF_TRANSPROXY_REFRESH, false);
    }

    public static boolean expandedNotifications() {
        return prefs.getBoolean(PREF_EXPANDED_NOTIFICATIONS, true);
    }

    public static boolean useDebugLogging() {
        return prefs.getBoolean(PREF_ENABLE_LOGGING, false);
    }

    public static boolean persistNotifications() {
        return prefs.getBoolean(PREF_PERSIST_NOTIFICATIONS, true);
    }

    public static boolean allowBackgroundStarts() {
        return prefs.getBoolean(PREF_ALLOW_BACKGROUND_STARTS, true);
    }

    public static boolean openProxyOnAllInterfaces() {
        return prefs.getBoolean(PREF_OPEN_PROXY_ON_ALL_INTERFACES, false);
    }

    public static boolean useVpn() {
        return prefs.getBoolean(PREF_USE_VPN, false);
    }

    public static void putUseVpn(boolean value) {
        putBoolean(PREF_USE_VPN, value);
    }

    public static boolean startOnBoot() {
        return prefs.getBoolean(PREF_START_ON_BOOT, true);
    }

    public static void putStartOnBoot(boolean value) {
        putBoolean(PREF_START_ON_BOOT, value);
    }
    
    public static String getExitNodes ()
    {
    	return prefs.getString(PREF_EXIT_NODES, "");
    }
    
    public static void setExitNodes (String exits)
    {
    	putString(PREF_EXIT_NODES,exits);
    }

    private static String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i != list.size() - 1) {
                sb.append("|");
            }
        }
        return sb.toString();
    }
}
