package com.mycompany.sesuaitugas.objects;

import com.mycompany.sesuaitugas.dao.Identifiable;

/**
 * Data mahasiswa untuk dashboard admin (koleksi {@code mahasiswa}).
 * Mengimplementasikan {@link Identifiable} agar dapat dikelola oleh
 * {@link GenericDAO}.
 */
public class Mahasiswa implements Identifiable {

    private String nim;
    private String idMahasiswa;
    private String nama;
    private String jurusan;
    private String password;
    private String email;
    /** Hash SHA-256 dari UID kartu RFID — null jika belum didaftarkan. */
    private String rfidHash;
    /** RSA public key (Base64 X.509 format). */
    private String rsaPublicKey;
    /** RSA private key terenkripsi dengan password (Base64 AES-GCM). */
    private String rsaEncryptedPrivateKey;

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
     * {@inheritDoc} - mengembalikan {@code idMahasiswa} sebagai ID unik entitas ini.
     * Dipetakan ke {@code _id} MongoDB untuk query efisien.
     */
    @org.bson.codecs.pojo.annotations.BsonId
    @Override
    public String getId() {
        return idMahasiswa;
    }

    /** Setter untuk BsonId decode — map _id ke idMahasiswa. */
    public void setId(String id) {
        this.idMahasiswa = id;
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

    public String getRfidHash() {
        return rfidHash;
    }

    public void setRfidHash(String rfidHash) {
        this.rfidHash = rfidHash;
    }

    public String getRsaPublicKey() { return rsaPublicKey; }
    public void setRsaPublicKey(String rsaPublicKey) { this.rsaPublicKey = rsaPublicKey; }

    public String getRsaEncryptedPrivateKey() { return rsaEncryptedPrivateKey; }
    public void setRsaEncryptedPrivateKey(String k) { this.rsaEncryptedPrivateKey = k; }

    /** RSA private key yang sudah didekripsi — hanya ada di session login (tidak disimpan ke DB). */
    private transient java.security.PrivateKey transientPrivateKey;

    @org.bson.codecs.pojo.annotations.BsonIgnore
    public java.security.PrivateKey getTransientPrivateKey() { return transientPrivateKey; }

    @org.bson.codecs.pojo.annotations.BsonIgnore
    public void setTransientPrivateKey(java.security.PrivateKey k) { this.transientPrivateKey = k; }
}
