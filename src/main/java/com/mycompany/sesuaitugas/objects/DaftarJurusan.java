package com.mycompany.sesuaitugas.objects;

/**
 * Daftar jurusan/program studi resmi untuk validasi dan combo box di GUI.
 */
public final class DaftarJurusan {

    /** Item pertama pada combo registrasi - bukan jurusan sungguhan. */
    public static final String PILIH_PLACEHOLDER = "--- Pilih jurusan ---";

    /**
     * Daftar jurusan (sesuai input kampus). Urutan dipakai untuk seed user default.
     */
    public static final String[] PILIHAN = {
            "S1 Teknik Mesin",
            "S1 Teknik Informatika",
            "S1 Sistem Informasi",
            "S1 Sains Data",
            "S1 Psikologi",
            "S1 Pendidikan Guru Sekolah Dasar",
            "S1 Manajemen",
            "S1 Ilmu Komunikasi",
            "S1 Hukum",
            "S1 Akuntansi",
            "Pendidikan Profesi Bidan",
            "D4 Kebidanan",
            "D4 Akuntasi Sektor Publik ",
            "D4 Teknik Informatika ",
            "D3 Teknik Mesin",
            "D3 Teknik Elektronika",
            "D3 Teknik Komputer",
            "D3 Perhotelan",
            "D3 Keperawatan",
            "D3 Farmasi",
            "D3 Desain Komunikasi Viusal",
            "D3 Akuntansi"
    };

    private DaftarJurusan() {
    }

    /** Model combo untuk form yang menyertakan placeholder di indeks 0. */
    public static String[] opsiUntukFormDaftar() {
        String[] a = new String[PILIHAN.length + 1];
        a[0] = PILIH_PLACEHOLDER;
        System.arraycopy(PILIHAN, 0, a, 1, PILIHAN.length);
        return a;
    }

    /**
     * Mengembalikan teks jurusan persis seperti di {@link #PILIHAN} (termasuk spasi
     * di ujung
     * jika ada di daftar), atau {@code null} jika tidak cocok.
     */
    public static String toCanonical(String jurusan) {
        if (jurusan == null || jurusan.trim().isEmpty() || PILIH_PLACEHOLDER.equals(jurusan)) {
            return null;
        }
        for (String j : PILIHAN) {
            if (j.equals(jurusan)) {
                return j;
            }
        }
        String t = jurusan.trim();
        for (String j : PILIHAN) {
            if (j.trim().equals(t)) {
                return j;
            }
        }
        return null;
    }

    public static boolean isValidSelection(String jurusan) {
        return toCanonical(jurusan) != null;
    }
}
