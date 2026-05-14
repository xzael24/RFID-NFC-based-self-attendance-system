package com.mycompany.sesuaitugas.objects;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;


public class MongoManager {
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static final String DATABASE_NAME = "DB_pemkom";

    public static MongoDatabase getDatabase() {
        if (database == null) {
            // Konfigurasi CodecRegistry untuk pemetaan POJO otomatis (Standard Industry)
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            try {
                // Inisiasi koneksi ke MongoDB Localhost (Driver 5.0.0)
                mongoClient = MongoClients.create("mongodb://localhost:27017");

                // Simpan database dengan registry yang sudah dikonfigurasi
                database = mongoClient.getDatabase(DATABASE_NAME).withCodecRegistry(pojoCodecRegistry);

                // Melakukan ping untuk memastikan server benar-benar tersambung
                database.runCommand(new Document("ping", 1));

                System.out.printf("Berhasil terhubung ke MongoDB (Database: %s)\n", DATABASE_NAME);
            } catch (Exception e) {
                System.err.println("Gagal terhubung ke MongoDB. Pastikan server MongoDB berjalan di localhost:27017");
                e.printStackTrace();
            }
        }
        if (database == null) {
            throw new IllegalStateException(
                    "MongoDB tidak tersedia (localhost:27017). Jalankan layanan MongoDB lalu coba lagi.");
        }
        return database;
    }
}