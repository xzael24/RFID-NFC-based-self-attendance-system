package com.mycompany.sesuaitugas.dao;

import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Kontrak dasar CRUD yang generik dan type-safe.
 * <p>
 * Pembatasan {@code <T extends Identifiable>} memastikan bahwa hanya entitas
 * yang memiliki ID unik yang dapat dikelola melalui DAO ini, sehingga
 * operasi update field parsial (patch) dapat dilakukan dengan aman
 * tanpa mengganti seluruh dokumen.
 * </p>
 *
 * @param <T> tipe entitas - harus mengimplementasikan {@link Identifiable}
 */
public interface BaseDAO<T extends Identifiable> {

    // ─── Operasi CRUD Dasar ───────────────────────────────────────────────

    /** Simpan entitas baru ke koleksi. */
    void save(T entity);

    /**
     * Ganti seluruh dokumen yang cocok dengan {@code filter} menggunakan
     * {@code entity}.
     * Gunakan {@link #updateFields(Bson, Document)} untuk update parsial.
     */
    void update(Bson filter, T entity);

    /**
     * Update field-field tertentu saja tanpa mengganti seluruh dokumen.
     * Contoh penggunaan:
     * 
     * <pre>
     * dao.updateFields(Filters.eq("nim", "123"), Updates.set("nama", "Budi"));
     * </pre>
     *
     * @param filter  kriteria dokumen yang akan diupdate
     * @param updates operator update MongoDB (misal: {@code Updates.set(...)})
     */
    void updateFields(Bson filter, Document updates);

    /** Hapus satu dokumen yang cocok dengan {@code filter}. */
    void delete(Bson filter);

    // ─── Operasi Searching / Reading ─────────────────────────────────────

    /** Ambil semua dokumen dalam koleksi. */
    List<T> findAll();

    /** Cari satu dokumen spesifik berdasarkan {@code filter}. */
    T findOne(Bson filter);

    /** Cari banyak dokumen berdasarkan {@code filter}. */
    List<T> findMany(Bson filter);
}
