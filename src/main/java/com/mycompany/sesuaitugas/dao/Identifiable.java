package com.mycompany.sesuaitugas.dao;

/**
 * Kontrak generic bound untuk semua entitas yang disimpan di MongoDB.
 * Memastikan setiap entitas memiliki identifier unik sehingga GenericDAO<T>
 * dapat menjamin type-safety pada operasi CRUD.
 */
public interface Identifiable {
    /** Kembalikan ID unik dokumen (digunakan sebagai kunci filter). */
    String getId();
}
