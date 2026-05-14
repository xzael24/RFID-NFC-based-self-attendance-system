package com.mycompany.sesuaitugas.objects;

import com.mongodb.client.model.Filters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * CRUD log absensi pada koleksi {@code log_absensi}.
 */
public class LogAbsensiService {

    private final GenericDAO<LogAbsensi> dao;

    public LogAbsensiService() {
        this.dao = new GenericDAO<>("log_absensi", LogAbsensi.class);
    }

    public List<LogAbsensi> findAll() {
        List<LogAbsensi> list = dao.findAll();
        Collections.sort(list, new Comparator<LogAbsensi>() {
            @Override
            public int compare(LogAbsensi a, LogAbsensi b) {
                Date da = a.getWaktuTap();
                Date db = b.getWaktuTap();
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return db.compareTo(da);
            }
        });
        return list;
    }

    public List<LogAbsensi> findByKodeKelas(String kode) {
        if (kode == null || kode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<LogAbsensi> list = dao.findMany(Filters.eq("kodeKelas", kode.trim()));
        Collections.sort(list, new Comparator<LogAbsensi>() {
            @Override
            public int compare(LogAbsensi a, LogAbsensi b) {
                Date da = a.getWaktuTap();
                Date db = b.getWaktuTap();
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return db.compareTo(da);
            }
        });
        return list;
    }

    public void save(LogAbsensi log) {
        dao.save(log);
    }

    public void deleteByIdLog(String idLog) {
        dao.delete(Filters.eq("idLog", idLog));
    }
}
