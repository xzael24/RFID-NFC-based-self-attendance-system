package com.mycompany.sesuaitugas.objects;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

public class MongoManager {
    private static MongoClient mongoClient;
    private static final String DATABASE_NAME = "db_absensi_pemkom";

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
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