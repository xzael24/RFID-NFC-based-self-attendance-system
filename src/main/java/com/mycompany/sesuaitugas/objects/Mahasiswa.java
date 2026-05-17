package com.mycompany.sesuaitugas.objects;

import com.mycompany.sesuaitugas.dao.Identifiable;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

/**
 * Data mahasiswa untuk dashboard admin (koleksi {@code mahasiswa}).
 * Mengimplementasikan {@link Identifiable} agar dapat dikelola oleh
 * {@link GenericDAO}.
 */
public class Mahasiswa implements Identifiable {

    @BsonId
    private ObjectId _id;
    private String nim;
    private String idMahasiswa;
    private String nama;
    private String jurusan;
    private String password;
    private String email;

    public Mahasiswa() {
    }

    public Mahasiswa(String nim, String idMahasiswa, String nama, String jurusan) {
        this.nim = nim;
        this.idMahasiswa = idMahasiswa;
        this.nama = nama;
        this.jurusan = jurusan;
    }

    public Mahasiswa(String nim, String idMahasiswa, String nama, String jurusan, String password) {
        this.nim = nim;
        this.idMahasiswa = idMahasiswa;
        this.nama = nama;
        this.jurusan = jurusan;
        this.password = password;
    }

    public Mahasiswa(String nim, String idMahasiswa, String nama, String jurusan, String password, String email) {
        this.nim = nim;
        this.idMahasiswa = idMahasiswa;
        this.nama = nama;
        this.jurusan = jurusan;
        this.password = password;
        this.email = email;
    }

    /**
     * {@inheritDoc} - mengembalikan {@code idMahasiswa} sebagai ID unik entitas
     * ini.
     */
    @BsonIgnore
    @Override
    public String getId() {
        return idMahasiswa;
    }

    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public String getIdMahasiswa() {
        return idMahasiswa;
    }

    public void setIdMahasiswa(String idMahasiswa) {
        this.idMahasiswa = idMahasiswa;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getJurusan() {
        return jurusan;
    }

    public void setJurusan(String jurusan) {
        this.jurusan = jurusan;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
