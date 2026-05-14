package com.mycompany.sesuaitugas.objects;

import com.mongodb.client.model.Filters;
import java.util.List;
import java.util.UUID;

/**
 * CRUD mahasiswa pada koleksi {@code mahasiswa}.
 */
public class MahasiswaService {

    private final GenericDAO<Mahasiswa> dao;

    public MahasiswaService() {
        this.dao = new GenericDAO<>("mahasiswa", Mahasiswa.class);
    }

    public List<Mahasiswa> findAll() {
        return dao.findAll();
    }

    public Mahasiswa findByNim(String nim) {
        if (nim == null || nim.trim().isEmpty()) {
            return null;
        }
        return dao.findOne(Filters.eq("nim", nim.trim()));
    }

    private static String generateIdMahasiswa() {
        return "MHS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    public void save(Mahasiswa m) {
        if (m.getIdMahasiswa() == null || m.getIdMahasiswa().trim().isEmpty()) {
            m.setIdMahasiswa(generateIdMahasiswa());
        }
        dao.save(m);
    }

    /** Update berdasarkan NIM yang dipakai saat baris dipilih (kunci lama). */
    public void updateByNim(String nimLama, Mahasiswa dataBaru) {
        if (dataBaru.getIdMahasiswa() == null || dataBaru.getIdMahasiswa().trim().isEmpty()) {
            Mahasiswa old = findByNim(nimLama);
            if (old != null && old.getIdMahasiswa() != null && !old.getIdMahasiswa().trim().isEmpty()) {
                dataBaru.setIdMahasiswa(old.getIdMahasiswa());
            } else {
                dataBaru.setIdMahasiswa(generateIdMahasiswa());
            }
        }
        dao.update(Filters.eq("nim", nimLama.trim()), dataBaru);
    }

    public void deleteByNim(String nim) {
        dao.delete(Filters.eq("nim", nim.trim()));
    }
}
