package com.mycompany.sesuaitugas.objects;

import com.mongodb.client.model.Filters;
import java.util.List;

public class KelasService {

    private final GenericDAO<Kelas> dao;
    private final GenericDAO<JadwalKelas> jadwalDao;

    public KelasService() {
        this.dao = new GenericDAO<>("kelas", Kelas.class);
        this.jadwalDao = new GenericDAO<>("jadwal_kelas", JadwalKelas.class);
    }

    public List<Kelas> findAll() {
        return dao.findAll();
    }

    public Kelas findByKode(String kode) {
        if (kode == null || kode.trim().isEmpty()) {
            return null;
        }
        return dao.findOne(Filters.eq("kodeKelas", kode.trim()));
    }

    public void save(Kelas k) {
        dao.save(k);
    }

    public void updateByKode(String kodeLama, Kelas baru) {
        dao.update(Filters.eq("kodeKelas", kodeLama.trim()), baru);
    }

    public void deleteByKode(String kode) {
        String k = kode.trim();
        for (JadwalKelas j : jadwalDao.findMany(Filters.eq("kodeKelas", k))) {
            jadwalDao.delete(Filters.eq("id", j.getId()));
        }
        dao.delete(Filters.eq("kodeKelas", k));
    }
}
