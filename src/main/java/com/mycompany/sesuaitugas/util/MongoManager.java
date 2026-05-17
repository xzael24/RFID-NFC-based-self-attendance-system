package com.mycompany.sesuaitugas.util;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 * Singleton manager untuk koneksi MongoDB.
 * <p>
 * Menggunakan lazy initialization agar koneksi hanya dibuka saat pertama kali
 * dibutuhkan.
 * Pastikan memanggil {@link #closeConnection()} saat aplikasi ditutup untuk
 * melepaskan resource koneksi dengan benar.
 * </p>
 */
public class MongoManager {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static final String DATABASE_NAME = "DB_pemkom";

    /** Cegah instantiasi - kelas ini bersifat utility/singleton statis. */
    private MongoManager() {
    }

    /**
     * Mengembalikan instance {@link MongoDatabase} yang aktif.
     * Koneksi dibuka sekali (lazy) dan digunakan kembali pada pemanggilan
     * berikutnya.
     *
     * @return instance database yang siap digunakan
     * @throws IllegalStateException jika MongoDB tidak dapat dihubungi
     */
    public static MongoDatabase getDatabase() {
        if (database == null) {
            // Konfigurasi CodecRegistry untuk pemetaan POJO otomatis (standar industri)
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

            try {
                // Inisiasi koneksi ke MongoDB localhost (Driver 5.x)
                mongoClient = MongoClients.create("mongodb://localhost:27017");

                // Simpan database dengan codec registry yang sudah dikonfigurasi
                database = mongoClient.getDatabase(DATABASE_NAME).withCodecRegistry(pojoCodecRegistry);

            } catch (Exception e) {
                System.err.println("Gagal terhubung ke MongoDB. Pastikan server MongoDB berjalan di localhost:27017");
                e.printStackTrace();
                database = null;
            }
        }

        if (database == null) {
            throw new IllegalStateException(
                    "MongoDB tidak tersedia (localhost:27017). Jalankan layanan MongoDB lalu coba lagi.");
        }
        return database;
    }

    /**
     * Menutup koneksi MongoDB dan mereset state singleton.
     * Panggil method ini saat aplikasi ditutup (misalnya dari WindowListener atau
     * shutdown hook).
     */
    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            System.out.println("Koneksi MongoDB ditutup.");
        }
    }
}
