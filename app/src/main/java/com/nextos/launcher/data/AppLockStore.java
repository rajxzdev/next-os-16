package com.nextos.launcher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * PIN hash + locked package list. Unlock tokens live only in memory.
 */
public final class AppLockStore {

    private static final String PREF = "next_os_lock";
    private static final String K_PIN = "pin_sha";
    private static final String K_APPS = "locked_apps";

    private static final Set<String> sessionUnlock = new HashSet<>();

    private final SharedPreferences sp;

    public AppLockStore(Context context) {
        this.sp = context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public boolean hasPin() {
        return !TextUtils.isEmpty(sp.getString(K_PIN, null));
    }

    public void setPin(String pin) {
        sp.edit().putString(K_PIN, sha(pin)).apply();
    }

    public boolean verifyPin(String pin) {
        String stored = sp.getString(K_PIN, null);
        return stored != null && stored.equals(sha(pin));
    }

    public Set<String> lockedPackages() {
        return new HashSet<>(sp.getStringSet(K_APPS, new HashSet<>()));
    }

    public boolean isLocked(String pkg) {
        return pkg != null && lockedPackages().contains(pkg);
    }

    public void setLocked(String pkg, boolean locked) {
        Set<String> set = lockedPackages();
        if (locked) {
            set.add(pkg);
        } else {
            set.remove(pkg);
            sessionUnlock.remove(pkg);
        }
        sp.edit().putStringSet(K_APPS, set).apply();
    }

    public void unlockSession(String pkg) {
        if (pkg != null) {
            sessionUnlock.add(pkg);
        }
    }

    public boolean isSessionUnlocked(String pkg) {
        return pkg != null && sessionUnlock.contains(pkg);
    }

    public void clearSession() {
        sessionUnlock.clear();
    }

    public boolean needsGate(String pkg) {
        return isLocked(pkg) && !isSessionUnlocked(pkg);
    }

    private static String sha(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("nextos16|" + pin).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(d, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf((pin + "nextos16").hashCode());
        }
    }
}
