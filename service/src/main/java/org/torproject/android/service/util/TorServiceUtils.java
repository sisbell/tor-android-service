/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals
.com/guardian */
/* See LICENSE for licensing information */
package org.torproject.android.service.util;

import android.content.Context;
import android.content.SharedPreferences;
import org.torproject.android.service.OrbotConstants;
import org.torproject.android.service.TorServiceConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.Runtime.getRuntime;

public class TorServiceUtils implements TorServiceConstants {

    public static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(OrbotConstants.PREF_TOR_SHARED_PREFS,
                Context.MODE_MULTI_PROCESS);
    }
}
