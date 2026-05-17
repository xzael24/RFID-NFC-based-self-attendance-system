package com.mycompany.sesuaitugas.util;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Kelas utilitas untuk menguji koneksi ke MongoDB.
 * <p>
 * Jalankan method {@code main} untuk memverifikasi bahwa server MongoDB
 * aktif dan database dapat dijangkau. Berguna untuk debugging koneksi
 * sebelum menjalankan aplikasi utama.
 * </p>
 */
public class TesKoneksi {

    /** Cegah instantiasi - kelas ini hanya berisi method statis. */
    private TesKoneksi() {
    }

    /**
     * Melakukan ping ke server MongoDB melalui {@link MongoManager}
     * dan mencetak hasilnya ke konsol.
     *
     * @return {@code true} jika koneksi berhasil, {@code false} jika gagal
     */
    public static boolean tesKoneksi() {
        try {
            MongoDatabase db = MongoManager.getDatabase();
            db.runCommand(new Document("ping", 1));
            System.out.println("Koneksi MongoDB berhasil! Database: " + db.getName());
            return true;
        } catch (Exception e) {
            System.err.println("Koneksi MongoDB GAGAL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Entry point untuk menjalankan tes koneksi secara mandiri.
     * <p>
     * Cara pakai: {@code java com.mycompany.util.TesKoneksi}
     * </p>
     */
    public static void main(String[] args) {
        System.out.println("=== Tes Koneksi MongoDB ===");
        boolean berhasil = tesKoneksi();

        if (berhasil) {
            System.out.println("Status: OK - MongoDB siap digunakan.");
        } else {
            System.out.println("Status: GAGAL - periksa apakah MongoDB sudah berjalan di localhost:27017.");
        }

        // Tutup koneksi setelah tes selesai
        MongoManager.closeConnection();
    }
}
