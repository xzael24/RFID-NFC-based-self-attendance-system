package com.mycompany.sesuaitugas.objects;

/**
 * Data mahasiswa untuk dashboard admin (koleksi {@code mahasiswa}).
 */
public class Mahasiswa {

    private String nim;
    private String idMahasiswa;
    private String nama;
    private String jurusan;

    public Mahasiswa() {
    }

    public Mahasiswa(String nim, String idMahasiswa, String nama, String jurusan) {
        this.nim = nim;
        this.idMahasiswa = idMahasiswa;
        this.nama = nama;
        this.jurusan = jurusan;
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
}
