package com.mycompany.sesuaitugas.services;

import com.mycompany.sesuaitugas.dao.GenericDAO;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mongodb.client.model.Filters;
import java.util.List;

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

    public Mahasiswa findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return dao.findOne(Filters.eq("email", email.trim()));
    }

    private int getNextIdMahasiswa() {
        List<Mahasiswa> all = dao.findAll();
        int maxId = 0;
        for (Mahasiswa m : all) {
            try {
                int current = Integer.parseInt(m.getIdMahasiswa());
                if (current > maxId) {
                    maxId = current;
                }
            } catch (NumberFormatException e) {
                // skip non-numeric IDs
            }
        }
        return maxId + 1;
    }

    public void save(Mahasiswa m) {
        if (m.getIdMahasiswa() == null || m.getIdMahasiswa().trim().isEmpty()) {
            m.setIdMahasiswa(String.valueOf(getNextIdMahasiswa()));
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
                dataBaru.setIdMahasiswa(String.valueOf(getNextIdMahasiswa()));
            }
        }
        dao.update(Filters.eq("nim", nimLama.trim()), dataBaru);
    }

    public void deleteByNim(String nim) {
        dao.delete(Filters.eq("nim", nim.trim()));
    }
}
