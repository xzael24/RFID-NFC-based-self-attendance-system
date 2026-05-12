package com.mycompany.sesuaitugas.objects;

import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementasi Generic DAO untuk MongoDB yang efisien dan reusable.
 * @param <T>
 */
public class GenericDAO<T> implements BaseDAO<T> {
    private final MongoCollection<T> collection;
    // Konstruktor menerima nama koleksi dan kelas entitas untuk mapping otomatis
    public GenericDAO(String collectionName, Class<T> clazz) {
        this.collection = MongoManager.getDatabase().getCollection(collectionName, clazz);
    }

    @Override

    public void save(T entity) { // T disini artinya GenericObject
        dataList.add(entity);
        // Pada Pertemuan 5, clazz akan digunakan oleh MongoDB Driver 5.0.0 
        // untuk mapping POJO (Plain Old Java Object) secara otomatis [2, 7].
        System.out.printf("Menyimpan objek tipe: %s ke koleksi: %s\n", clazz.getSimpleName(), collectionName);
        
    public void save(T entity) {
        collection.insertOne(entity);

    }

    @Override
    public void update(Bson filter, T entity) {
        collection.replaceOne(filter, entity);
    }

    @Override
    public void delete(Bson filter) {
        collection.deleteOne(filter);
    }

    @Override
    public List<T> findAll() {
        return collection.find().into(new ArrayList<>());
    }

    @Override
    public T findOne(Bson filter) {
        return collection.find(filter).first();
    }

    @Override
    public List<T> findMany(Bson filter) {
        return collection.find(filter).into(new ArrayList<>());
    }
}