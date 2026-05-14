package com.mycompany.sesuaitugas.objects;

import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.List;

public class JadwalKelasService {

    private final GenericDAO<JadwalKelas> dao;

    public JadwalKelasService() {
        this.dao = new GenericDAO<>("jadwal_kelas", JadwalKelas.class);
    }

    public List<JadwalKelas> findAll() {
        return dao.findAll();
    }

    public List<JadwalKelas> findByKodeKelas(String kodeKelas) {
        if (kodeKelas == null || kodeKelas.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return dao.findMany(Filters.eq("kodeKelas", kodeKelas.trim()));
    }

    public void save(JadwalKelas j) {
        dao.save(j);
    }

    public void updateById(String id, JadwalKelas j) {
        dao.update(Filters.eq("id", id), j);
    }

    public void deleteById(String id) {
        dao.delete(Filters.eq("id", id));
    }
}
