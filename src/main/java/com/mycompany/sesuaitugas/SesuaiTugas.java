/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.sesuaitugas;

/**
 *
 * @author AcerAG14
 */
import com.mycompany.sesuaitugas.objects.Karyawan;
import com.mycompany.sesuaitugas.objects.TesKoneksi;

public class SesuaiTugas {
    public static void main(String[] args) {
        Karyawan KR = new Karyawan();
        if(KR instanceof Karyawan){
            System.err.println("Karyawan Loaded");
        }else {
            System.err.println("Something else");
        }

        System.out.println("\nMenguji koneksi ke database...");
        TesKoneksi.testConnection();
    }
}
