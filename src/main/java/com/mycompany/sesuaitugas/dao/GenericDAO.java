package com.mycompany.sesuaitugas.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mycompany.sesuaitugas.util.MongoManager;

import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementasi generic DAO yang efisien, type-safe, dan reusable untuk MongoDB.
 *
 * <p>
 * Dengan pembatasan {@code <T extends Identifiable>}, kelas ini memastikan
 * hanya entitas yang memiliki identifier unik yang dapat dikelola.
 * Seluruh operasi CRUD didelegasikan ke {@link MongoCollection<T>} sehingga
 * tidak ada duplikasi kode antar-service.
 * </p>
 *
 * <p>
 * <b>Cara penggunaan (reusability):</b>
 * </p>
 * 
 * <pre>
 * // Cukup buat instance baru - tidak perlu tulis ulang logika CRUD:
 * GenericDAO&lt;Mahasiswa&gt; dao = new GenericDAO&lt;&gt;("mahasiswa", Mahasiswa.class);
 * GenericDAO&lt;Kelas&gt; dao = new GenericDAO&lt;&gt;("kelas", Kelas.class);
 * </pre>
 *
 * @param <T> tipe entitas - harus mengimplementasikan {@link Identifiable}
 */
public class GenericDAO<T extends Identifiable> implements BaseDAO<T> {

    private final MongoCollection<T> collection;

    /**
     * Konstruktor menerima nama koleksi dan kelas entitas untuk mapping POJO
     * otomatis.
     *
     * @param collectionName nama koleksi MongoDB
     * @param clazz          kelas entitas (digunakan codec POJO)
     */
    public GenericDAO(String collectionName, Class<T> clazz) {
        this.collection = MongoManager.getDatabase().getCollection(collectionName, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public void save(T entity) {
        collection.insertOne(entity);
    }

    /**
     * {@inheritDoc}
     * Mengganti seluruh dokumen (full replace). Untuk update parsial,
     * gunakan {@link #updateFields(Bson, Document)}.
     */
    @Override
    public void update(Bson filter, T entity) {
        collection.replaceOne(filter, entity);
    }

    /**
     * {@inheritDoc}
     * Update hanya field-field tertentu menggunakan operator MongoDB
     * (mis. {@code $set}, {@code $inc}) tanpa mengganti keseluruhan dokumen.
     * Ini lebih efisien dibanding {@code replaceOne} jika hanya satu field
     * yang berubah.
     */
    @Override
    public void updateFields(Bson filter, Document updates) {
        collection.updateOne(filter, new Document("$set", updates), new UpdateOptions().upsert(false));
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Bson filter) {
        collection.deleteOne(filter);
    }

    /** {@inheritDoc} */
    @Override
    public List<T> findAll() {
        return collection.find().into(new ArrayList<>());
    }

    /** {@inheritDoc} */
    @Override
    public T findOne(Bson filter) {
        return collection.find(filter).first();
    }

    /** {@inheritDoc} */
    @Override
    public List<T> findMany(Bson filter) {
        return collection.find(filter).into(new ArrayList<>());
    }
}
