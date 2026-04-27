/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sesuaitugas.objects;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 *
 * @author mnish
 */
/**
 * Mengelola koneksi database NoSQL (Pertemuan 5)
 */
public class MongoManager {
    private static MongoClient mongoClient;
    private static final String DATABASE_NAME = "db_absensi_pemkom";

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
        }
        return mongoClient.getDatabase(DATABASE_NAME);
    }
}