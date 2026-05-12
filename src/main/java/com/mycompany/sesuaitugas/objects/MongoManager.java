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
    private static final String DATABASE_NAME = "DB_pemkom";

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            try {
                // Menggunakan default connection string untuk MongoDB versi 5.0.0
                mongoClient = MongoClients.create("mongodb://localhost:27017");
                
                // Melakukan ping untuk memastikan server benar-benar tersambung
                MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
                database.runCommand(new Document("ping", 1));
                
                System.out.printf("Berhasil terhubung ke MongoDB (Database: %s)\n", DATABASE_NAME);
            } catch (Exception e) {
                System.err.println("Gagal terhubung ke MongoDB. Pastikan server MongoDB berjalan di localhost:27017");
                e.printStackTrace();
            }
            // Konfigurasi CodecRegistry untuk pemetaan POJO otomatis (Standard Industry)
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            // Inisiasi koneksi ke MongoDB Localhost (Driver 5.0.0)
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            
            // Mengembalikan database dengan registry yang sudah dikonfigurasi
            return mongoClient.getDatabase(DATABASE_NAME).withCodecRegistry(pojoCodecRegistry);
        }
        return mongoClient.getDatabase(DATABASE_NAME);
    }
}