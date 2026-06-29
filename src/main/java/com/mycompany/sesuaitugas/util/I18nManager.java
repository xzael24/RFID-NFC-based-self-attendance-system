package com.mycompany.sesuaitugas.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Manager untuk internationalization (i18n).
 * Mengelola pemilihan bahasa (Indonesia/English) dan loading resource bundle.
 *
 * <p>Penggunaan:
 * <pre>
 * I18nManager.setLocale(new Locale("id", "ID"));
 * String msg = I18nManager.get("admin.title"); // "Dashboard Admin"
 * </pre>
 */
public final class I18nManager {

    private static Locale currentLocale = new Locale("id", "ID"); // Default: Indonesia
    private static ResourceBundle bundle;

    static {
        loadBundle();
    }

    private I18nManager() {
    }

    /**
     * Load resource bundle sesuai locale yang dipilih.
     */
    private static void loadBundle() {
        try {
            bundle = ResourceBundle.getBundle("messages", currentLocale);
            System.out.println("[i18n] Loaded: messages_" + currentLocale.getLanguage() + ".properties");
        } catch (Exception e) {
            System.err.println("[i18n] Gagal load bundle: " + e.getMessage());
            bundle = null;
        }
    }

    /**
     * Set locale dan reload bundle.
     *
     * @param locale baru (contoh: new Locale("id", "ID"), new Locale("en", "US"))
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadBundle();
    }

    /**
     * Get message dari resource bundle.
     *
     * @param key kunci message (contoh: "admin.title")
     * @return string dari properties file, atau key itu sendiri jika tidak ditemukan
     */
    public static String get(String key) {
        if (bundle == null) {
            return key;
        }
        try {
            return bundle.getString(key);
        } catch (java.util.MissingResourceException e) {
            System.err.println("[i18n] Missing key: " + key);
            return key;
        }
    }

    /**
     * Get current locale.
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Get current language code (contoh: "id", "en").
     */
    public static String getLanguage() {
        return currentLocale.getLanguage();
    }

    /**
     * Check apakah current locale adalah Indonesia.
     */
    public static boolean isIndonesian() {
        return "id".equals(currentLocale.getLanguage());
    }

    /**
     * Check apakah current locale adalah English.
     */
    public static boolean isEnglish() {
        return "en".equals(currentLocale.getLanguage());
    }
}
