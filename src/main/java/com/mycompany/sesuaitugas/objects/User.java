package com.mycompany.sesuaitugas.objects;

import com.mycompany.sesuaitugas.dao.Identifiable;

/**
 * Model User untuk autentikasi login admin/mahasiswa.
 * Disimpan di koleksi "admin" pada MongoDB.
 * Mengimplementasikan {@link Identifiable} agar dapat dikelola oleh
 * {@link GenericDAO}.
 */
public class User implements Identifiable {

    private String email;
    private String password;
    private String role; // "admin" atau "user"
    /** Jurusan/program studi (mahasiswa); admin boleh null. */
    private String jurusan;
    /** Nama lengkap tampilan (mahasiswa/admin). */
    private String nama;
    /** NIM mahasiswa (akun role user); admin boleh null. */
    private String nim;
    /** RSA public key (Base64 X.509 format). */
    private String rsaPublicKey;
    /** RSA private key terenkripsi dengan password (Base64 AES-GCM). */
    private String rsaEncryptedPrivateKey;

    public User() {
    }

    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public User(String email, String password, String role, String jurusan) {
        this(email, password, role, jurusan, null);
    }

    public User(String email, String password, String role, String jurusan, String nama) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.jurusan = jurusan;
        this.nama = nama;
    }

    /**
     * {@inheritDoc} - mengembalikan {@code email} sebagai ID unik entitas ini.
     * Dipetakan ke {@code _id} MongoDB untuk query efisien.
     */
    @org.bson.codecs.pojo.annotations.BsonId
    @Override
    public String getId() {
        return email;
    }

    /** Setter untuk BsonId decode — map _id ke email. */
    public void setId(String id) {
        this.email = id;
    }

    // ─── Getters & Setters ───

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJurusan() {
        return jurusan;
    }

    public void setJurusan(String jurusan) {
        this.jurusan = jurusan;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public String getRsaPublicKey() { return rsaPublicKey; }
    public void setRsaPublicKey(String rsaPublicKey) { this.rsaPublicKey = rsaPublicKey; }

    public String getRsaEncryptedPrivateKey() { return rsaEncryptedPrivateKey; }
    public void setRsaEncryptedPrivateKey(String k) { this.rsaEncryptedPrivateKey = k; }

    @Override
    public String toString() {
        return "User{email='" + email + "', nama='" + nama + "', nim='" + nim
                + "', role='" + role + "', jurusan='" + jurusan + "'}";
    }
}
