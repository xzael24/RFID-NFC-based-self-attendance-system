package com.mycompany.sesuaitugas.objects;

import java.util.Date;

/**
 * Catatan kehadiran / tap (koleksi {@code log_absensi}).
 * Menggunakan {@link Date} agar serialisasi MongoDB + codec POJO stabil di Java 8.
 */
public class LogAbsensi {

    private String idLog;
    private String nim;
    private String kodeKelas;
    private String uidRfid;
    private Date waktuTap;
    private String status;
    private String keterangan;

    public LogAbsensi() {
    }

    public LogAbsensi(String idLog, String nim, String kodeKelas, String uidRfid,
            Date waktuTap, String status, String keterangan) {
        this.idLog = idLog;
        this.nim = nim;
        this.kodeKelas = kodeKelas;
        this.uidRfid = uidRfid;
        this.waktuTap = waktuTap;
        this.status = status;
        this.keterangan = keterangan;
    }

    public String getIdLog() {
        return idLog;
    }

    public void setIdLog(String idLog) {
        this.idLog = idLog;
    }

    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public String getKodeKelas() {
        return kodeKelas;
    }

    public void setKodeKelas(String kodeKelas) {
        this.kodeKelas = kodeKelas;
    }

    public String getUidRfid() {
        return uidRfid;
    }

    public void setUidRfid(String uidRfid) {
        this.uidRfid = uidRfid;
    }

    public Date getWaktuTap() {
        return waktuTap;
    }

    public void setWaktuTap(Date waktuTap) {
        this.waktuTap = waktuTap;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKeterangan() {
        return keterangan;
    }

    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }
}
