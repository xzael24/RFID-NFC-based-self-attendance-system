package com.mycompany.sesuaitugas.util;

import com.mycompany.sesuaitugas.dao.GenericDAO;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.services.MahasiswaService;
import com.mongodb.client.model.Filters;
import java.util.List;

/**
 * Seeder data dummy — insert mahasiswa via MahasiswaService.save()
 * sehingga password di-hash & email dienkripsi sesuai service layer.
 */
public final class SeedDummyData {

    private SeedDummyData() {}

    public static void seed() {
        MahasiswaService svc = new MahasiswaService();

        // Hapus SEMUA data mahasiswa dulu, lalu seeding ulang
        // biar data lama (plaintext password) ga mengganggu
        GenericDAO<Mahasiswa> dao =
            new GenericDAO<>("mahasiswa", Mahasiswa.class);
        List<Mahasiswa> existing = dao.findAll();
        for (Mahasiswa m : existing) {
            dao.delete(Filters.eq("_id", m.getId()));
        }
        System.out.println("⚠ " + existing.size() + " data lama dihapus.");

        Object[][] data = {
            {"24090001", "MHS001", "Aditya Pratama",   "Teknik Informatika", "123", "aditya@student.uhn.ac.id", "A3F2B1C4"},
            {"24090002", "MHS002", "Bella Safira",      "Teknik Informatika", "123", "bella@student.uhn.ac.id",   "D5E6F7A8"},
            {"24090003", "MHS003", "Chandra Wijaya",    "Sistem Informasi",   "123", "chandra@student.uhn.ac.id", "1B2C3D4E"},
            {"24090004", "MHS004", "Dian Ayu Lestari",  "Sistem Informasi",   "123", "dian@student.uhn.ac.id",    "F9E8D7C6"},
            {"24090005", "MHS005", "Eko Prasetyo",      "Teknik Komputer",    "123", "eko@student.uhn.ac.id",     "4A5B6C7D"},
            {"24090006", "MHS006", "Fina Rahmawati",    "Teknik Komputer",    "123", "fina@student.uhn.ac.id",    "8E9F0A1B"},
            {"24090007", "MHS007", "Gilang Ramadhan",   "Teknik Informatika", "123", "gilang@student.uhn.ac.id",  "C2D3E4F5"},
            {"24090008", "MHS008", "Hesti Nurul Aini",  "Sistem Informasi",   "123", "hesti@student.uhn.ac.id",   "6A7B8C9D"},
            {"24090009", "MHS009", "Irfan Hakim",       "Teknik Informatika", "123", "irfan@student.uhn.ac.id",   "0E1F2A3B"},
            {"24090010", "MHS010", "Jasmine Putri",     "Teknik Komputer",    "123", "jasmine@student.uhn.ac.id", "5C6D7E8F"},
        };

        for (Object[] row : data) {
            Mahasiswa m = new Mahasiswa(
                (String) row[0], // nim
                (String) row[1], // idMahasiswa
                (String) row[2], // nama
                (String) row[3], // jurusan
                (String) row[4], // password (plaintext — akan di-hash oleh save())
                (String) row[5]  // email (plaintext — akan dienkripsi oleh save())
            );
            // Set rfidHash langsung (deterministik — tanpa salt)
            String uid = (String) row[6];
            String hash = HashUtil.hashDeterministic(uid);
            m.setRfidHash(hash);
            svc.save(m);
            System.out.println("✓ " + m.getNama() + " (" + m.getNim() + ") — UID: " + uid);
        }

        System.out.println("\n✅ " + data.length + " mahasiswa dummy berhasil dibuat.");
        System.out.println("   Login: NIM / email + password '123'");
    }
}
