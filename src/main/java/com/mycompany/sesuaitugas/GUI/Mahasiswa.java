package com.mycompany.sesuaitugas.gui;

import com.mycompany.sesuaitugas.services.AbsensiService;
import com.mycompany.sesuaitugas.util.RfidSerialListener;
import com.mycompany.sesuaitugas.util.I18nManager;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * Halaman absensi RFID/NFC untuk mahasiswa.
 * Alur: Pilih COM port → Hubungkan → tap kartu → absensi tersimpan ke MongoDB.
 * Fallback: input NIM manual juga tersedia.
 */
public class Mahasiswa extends javax.swing.JFrame {

    private final AbsensiService absensiService = new AbsensiService();
    private DefaultTableModel    logTableModel;
    private RfidSerialListener   rfidListener;
    private com.mycompany.sesuaitugas.objects.Mahasiswa loggedInMhs;

    public Mahasiswa() {
        initComponents();
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screen.width, screen.height);
        jPanel1.setPreferredSize(new java.awt.Dimension(screen.width, screen.height));
        setLocationRelativeTo(null);
        seedInitialData();
        setupClock();
        setupLogTable();
        setupPortCombo();
        setupListeners();
        refreshUITextMahasiswa();
        txtRfidInput.requestFocusInWindow();
    }

    /**
     * Constructor dengan data mahasiswa yang login.
     */
    public Mahasiswa(com.mycompany.sesuaitugas.objects.Mahasiswa mhs) {
        this();
        this.loggedInMhs = mhs;
        if (mhs != null) {
            String display = mhs.getNama() != null && !mhs.getNama().trim().isEmpty()
                    ? mhs.getNama().trim() : mhs.getNim();
            setTitle("Absensi RFID - " + display + " (" + mhs.getNim() + ")");
            loadRiwayatAbsensi(mhs.getNim());
        }
    }

    /** Load existing absensi records for the logged-in student into the table. */
    private void loadRiwayatAbsensi(String nim) {
        try {
            java.util.List<com.mycompany.sesuaitugas.objects.Absensi> list =
                    absensiService.findByNim(nim);
            logTableModel.setRowCount(0);
            for (com.mycompany.sesuaitugas.objects.Absensi a : list) {
                logTableModel.addRow(new Object[]{
                    a.getTanggal() + " " + a.getWaktu(),
                    a.getNim(),
                    a.getNama(),
                    a.getStatus()
                });
            }
        } catch (Exception ex) {
            System.err.println("Gagal muat riwayat absensi: " + ex.getMessage());
        }
    }

    private void setupClock() {
        updateClockLabels();
        new Timer(1000, e -> updateClockLabels()).start();
    }

    private void updateClockLabels() {
        Date now = new Date();
        lblClock.setText(new SimpleDateFormat("HH:mm:ss").format(now));
        lblDate.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID")).format(now));
    }

    private void setupLogTable() {
        logTableModel = (DefaultTableModel) tblLog.getModel();
    }

    private void setupPortCombo() {
        try {
            String[] ports = RfidSerialListener.getAvailablePorts();
            cbPort.setModel(new DefaultComboBoxModel<>(
                    ports.length > 0 ? ports : new String[]{"(Tidak ada port)"}));
            if (ports.length == 0) btnConnect.setEnabled(false);
        } catch (Throwable t) {
            cbPort.setModel(new DefaultComboBoxModel<>(new String[]{"(Library tidak tersedia)"}));
            btnConnect.setEnabled(false);
        }
    }

    private void seedInitialData() {
        com.mycompany.sesuaitugas.services.LoginService svc = new com.mycompany.sesuaitugas.services.LoginService();
        svc.ensureDefaultAdmin();
        svc.ensureDefaultMahasiswa();
        com.mycompany.sesuaitugas.util.SeedDummyData.seed();
    }

    private void setupListeners() {
        btnScan.addActionListener(e -> onManualScan());
        txtRfidInput.addActionListener(e -> onManualScan());
        btnAdminLogin.addActionListener(e -> onAdminLogin());
        btnConnect.addActionListener(e -> onToggleConnect());
        btnSimulasi.addActionListener(e -> onSimulasiRfid());
    }

    private void onSimulasiRfid() {
        String uid = javax.swing.JOptionPane.showInputDialog(this,
                "Masukkan UID kartu RFID (simulasi):\nContoh: A1B2C3D4 atau 12345678",
                "Simulasi RFID", javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (uid != null && !uid.trim().isEmpty()) {
            prosesUidDariSerial(uid.trim());
        }
    }

    private void onToggleConnect() {
        if (rfidListener != null && rfidListener.isRunning()) {
            rfidListener.stop();
            rfidListener = null;
            btnConnect.setText("Hubungkan");
            lblPortStatus.setText("Serial: Tidak terhubung");
            lblPortStatus.setForeground(Color.GRAY);
        } else {
            String port = (String) cbPort.getSelectedItem();
            if (port == null || port.startsWith("(")) {
                showStatus("Tidak ada COM port tersedia.", new Color(204, 0, 0));
                return;
            }
            rfidListener = new RfidSerialListener(port,
                    uid -> SwingUtilities.invokeLater(() -> prosesUidDariSerial(uid)));
            if (rfidListener.start()) {
                btnConnect.setText("Putuskan");
                lblPortStatus.setText("Serial: Terhubung ke " + port);
                lblPortStatus.setForeground(new Color(0, 153, 0));
                showStatus("Reader RFID aktif. Tempelkan kartu...", new Color(0, 102, 204));
            } else {
                rfidListener = null;
                showStatus("Gagal membuka " + port + ". Periksa koneksi Arduino.", new Color(204, 0, 0));
                lblPortStatus.setText("Serial: Gagal terhubung");
                lblPortStatus.setForeground(new Color(204, 0, 0));
            }
        }
    }

    private void prosesUidDariSerial(String uid) {
        showStatus("Memproses kartu: " + uid + " ...", new Color(0, 102, 204));
        java.security.PrivateKey privKey = (loggedInMhs != null) ? loggedInMhs.getTransientPrivateKey() : null;
        new javax.swing.SwingWorker<AbsensiService.HasilScan, Void>() {
            @Override protected AbsensiService.HasilScan doInBackground() {
                return (privKey != null) ? absensiService.prosesUid(uid, privKey) : absensiService.prosesUid(uid);
            }
            @Override protected void done() {
                try {
                    AbsensiService.HasilScan hasil = get();
                    if (hasil == null) { showStatus("Kartu tidak terdaftar: " + uid, new Color(204, 0, 0)); clearInfoPanel(); }
                    else { tampilkanHasilScan(hasil); }
                } catch (Exception ex) { showStatus("Error: " + ex.getMessage(), new Color(204, 0, 0)); }
            }
        }.execute();
    }

    private void onManualScan() {
        String input = txtRfidInput.getText().trim();
        if (input.isEmpty()) { showStatus("Masukkan NIM atau tempelkan kartu RFID.", new Color(204, 0, 0)); return; }
        showStatus("Memproses NIM: " + input + " ...", new Color(0, 102, 204));
        java.security.PrivateKey privKey = (loggedInMhs != null) ? loggedInMhs.getTransientPrivateKey() : null;
        new javax.swing.SwingWorker<AbsensiService.HasilScan, Void>() {
            @Override protected AbsensiService.HasilScan doInBackground() {
                com.mycompany.sesuaitugas.objects.Mahasiswa mhs =
                        absensiService.getMahasiswaService().findByNim(input);
                // Also set transient key on the found mhs so signing works
                if (mhs != null && privKey != null) {
                    try {
                        java.lang.reflect.Method setter = mhs.getClass().getMethod("setTransientPrivateKey", java.security.PrivateKey.class);
                        setter.invoke(mhs, privKey);
                    } catch (Exception e) { /* ignore */ }
                }
                return mhs == null ? null : absensiService.prosesManual(mhs, privKey);
            }
            @Override protected void done() {
                try {
                    AbsensiService.HasilScan hasil = get();
                    if (hasil == null) { showStatus("NIM tidak ditemukan: " + input, new Color(204, 0, 0)); clearInfoPanel(); }
                    else { tampilkanHasilScan(hasil); txtRfidInput.setText(""); }
                } catch (Exception ex) { showStatus("Error: " + ex.getMessage(), new Color(204, 0, 0)); }
                txtRfidInput.requestFocusInWindow();
            }
        }.execute();
    }

    private void tampilkanHasilScan(AbsensiService.HasilScan hasil) {
        com.mycompany.sesuaitugas.objects.Mahasiswa mhs = hasil.mahasiswa;
        lblNamaValue.setText(nullToEmpty(mhs.getNama()));
        lblNimValue.setText(nullToEmpty(mhs.getNim()));
        lblJurusanValue.setText(nullToEmpty(mhs.getJurusan()));
        lblIdValue.setText(nullToEmpty(mhs.getIdMahasiswa()));
        String waktu = hasil.absensi.getWaktu();
        String statusLabel = "HADIR";
        if (hasil.absensi.getSignature() != null && !hasil.absensi.getSignature().isEmpty()) {
            statusLabel = "HADIR \u2713 RSA";
        }
        logTableModel.insertRow(0, new Object[]{waktu, mhs.getNim(), mhs.getNama(), statusLabel});
        showStatus("Absensi berhasil! (tersimpan ke database)", new Color(0, 153, 0));
        lblStatusTime.setText("Tercatat pukul " + waktu);
        com.mycompany.sesuaitugas.util.SoundUtil.playSuccess();
    }

    private void showStatus(String msg, Color c) { lblStatus.setText(msg); lblStatus.setForeground(c); }

    private void clearInfoPanel() {
        lblNamaValue.setText("-"); lblNimValue.setText("-");
        lblJurusanValue.setText("-"); lblIdValue.setText("-"); lblStatusTime.setText("");
    }

    private void onAdminLogin() {
        Login loginFrame = new Login(this);
        loginFrame.setVisible(true);
    }

    private static String nullToEmpty(String s) { return s == null || s.trim().isEmpty() ? "-" : s; }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();
        lblClock = new javax.swing.JLabel();
        lblDate = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lblPortLabel = new javax.swing.JLabel();
        cbPort = new javax.swing.JComboBox();
        btnConnect = new javax.swing.JButton();
        lblPortStatus = new javax.swing.JLabel();
        btnSimulasi = new javax.swing.JButton();
        lblRfidLabel = new javax.swing.JLabel();
        txtRfidInput = new javax.swing.JTextField();
        btnScan = new javax.swing.JButton();
        btnLanguageID = new javax.swing.JButton();
        btnLanguageEN = new javax.swing.JButton();
        btnAdminLogin = new javax.swing.JButton();
        panelInfo = new javax.swing.JPanel();
        lblNamaLabel = new javax.swing.JLabel();
        lblNamaValue = new javax.swing.JLabel();
        lblNimLabel = new javax.swing.JLabel();
        lblNimValue = new javax.swing.JLabel();
        lblJurusanLabel = new javax.swing.JLabel();
        lblJurusanValue = new javax.swing.JLabel();
        lblIdLabel = new javax.swing.JLabel();
        lblIdValue = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        lblStatusTime = new javax.swing.JLabel();
        lblLogTitle = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblLog = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Absensi RFID - Mahasiswa");

        jPanel1.setBackground(new java.awt.Color(240, 248, 250));
        jPanel1.setPreferredSize(new java.awt.Dimension(1870, 1200));
        jPanel1.setLayout(null);

        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        lblTitle.setText("Absensi RFID / NFC");
        jPanel1.add(lblTitle);
        lblTitle.setBounds(60, 30, 500, 60);

        lblSubtitle.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblSubtitle.setText("Tempelkan kartu RFID/NFC atau masukkan NIM secara manual");
        jPanel1.add(lblSubtitle);
        lblSubtitle.setBounds(60, 90, 600, 35);

        lblClock.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        lblClock.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblClock.setText("00:00:00");
        jPanel1.add(lblClock);
        lblClock.setBounds(1540, 30, 120, 50);

        lblDate.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        lblDate.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblDate.setText("Sabtu, 17 Mei 2026");
        jPanel1.add(lblDate);
        lblDate.setBounds(1260, 80, 400, 30);
        jPanel1.add(jSeparator1);
        jSeparator1.setBounds(0, 140, 1870, 3);

        lblPortLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblPortLabel.setText("COM Port:");
        jPanel1.add(lblPortLabel);
        lblPortLabel.setBounds(60, 148, 80, 30);

        cbPort.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jPanel1.add(cbPort);
        cbPort.setBounds(145, 147, 160, 32);

        btnConnect.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnConnect.setText("Hubungkan");
        jPanel1.add(btnConnect);
        btnConnect.setBounds(315, 147, 120, 32);

        lblPortStatus.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblPortStatus.setText("Serial: Tidak terhubung");
        jPanel1.add(lblPortStatus);
        lblPortStatus.setBounds(450, 148, 250, 30);

        btnSimulasi.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSimulasi.setText("Simulasi RFID");
        jPanel1.add(btnSimulasi);
        btnSimulasi.setBounds(720, 147, 140, 32);

        lblRfidLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblRfidLabel.setText("RFID / NFC ID :");
        jPanel1.add(lblRfidLabel);
        lblRfidLabel.setBounds(60, 200, 200, 50);

        txtRfidInput.setFont(new java.awt.Font("Segoe UI", 0, 22)); // NOI18N
        txtRfidInput.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jPanel1.add(txtRfidInput);
        txtRfidInput.setBounds(270, 200, 570, 55);

        btnScan.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnScan.setText("Scan / Absen");
        jPanel1.add(btnScan);
        btnScan.setBounds(870, 200, 300, 55);

        btnLanguageID.setBackground(new java.awt.Color(240, 240, 224));
        btnLanguageID.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        btnLanguageID.setText("🇮🇩 ID");
        btnLanguageID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLanguageIDActionPerformed(evt);
            }
        });
        jPanel1.add(btnLanguageID);
        btnLanguageID.setBounds(1310, 60, 85, 45);

        btnLanguageEN.setBackground(new java.awt.Color(240, 240, 224));
        btnLanguageEN.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        btnLanguageEN.setText("🇬🇧 EN");
        btnLanguageEN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLanguageENActionPerformed(evt);
            }
        });
        jPanel1.add(btnLanguageEN);
        btnLanguageEN.setBounds(1410, 60, 85, 45);

        btnAdminLogin.setBackground(new java.awt.Color(220, 220, 220));
        btnAdminLogin.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        btnAdminLogin.setText("Admin Login");
        jPanel1.add(btnAdminLogin);
        btnAdminLogin.setBounds(1350, 20, 100, 28);

        panelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Informasi Mahasiswa", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 16))); // NOI18N
        panelInfo.setLayout(null);

        lblNamaLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblNamaLabel.setText("Nama :");
        panelInfo.add(lblNamaLabel);
        lblNamaLabel.setBounds(30, 40, 120, 35);

        lblNamaValue.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblNamaValue.setText("-");
        panelInfo.add(lblNamaValue);
        lblNamaValue.setBounds(160, 40, 500, 35);

        lblNimLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblNimLabel.setText("NIM :");
        panelInfo.add(lblNimLabel);
        lblNimLabel.setBounds(30, 85, 120, 35);

        lblNimValue.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblNimValue.setText("-");
        panelInfo.add(lblNimValue);
        lblNimValue.setBounds(160, 85, 500, 35);

        lblJurusanLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblJurusanLabel.setText("Jurusan :");
        panelInfo.add(lblJurusanLabel);
        lblJurusanLabel.setBounds(30, 130, 120, 35);

        lblJurusanValue.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblJurusanValue.setText("-");
        panelInfo.add(lblJurusanValue);
        lblJurusanValue.setBounds(160, 130, 500, 35);

        lblIdLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblIdLabel.setText("ID :");
        panelInfo.add(lblIdLabel);
        lblIdLabel.setBounds(30, 175, 120, 35);

        lblIdValue.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblIdValue.setText("-");
        panelInfo.add(lblIdValue);
        lblIdValue.setBounds(160, 175, 500, 35);

        jPanel1.add(panelInfo);
        panelInfo.setBounds(60, 290, 700, 280);

        lblStatus.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        lblStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblStatus.setText("Menunggu scan...");
        jPanel1.add(lblStatus);
        lblStatus.setBounds(800, 330, 600, 80);

        lblStatusTime.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblStatusTime.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jPanel1.add(lblStatusTime);
        lblStatusTime.setBounds(800, 410, 600, 35);

        lblLogTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblLogTitle.setText("Riwayat Absensi Hari Ini");
        jPanel1.add(lblLogTitle);
        lblLogTitle.setBounds(60, 590, 350, 40);

        tblLog.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        tblLog.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Waktu", "NIM", "Nama", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblLog.setRowHeight(30);
        jScrollPane1.setViewportView(tblLog);

        jPanel1.add(jScrollPane1);
        jScrollPane1.setBounds(60, 640, 1750, 500);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Refresh semua text UI di Mahasiswa sesuai locale.
     */
    private void refreshUITextMahasiswa() {
        lblTitle.setText(I18nManager.get("mahasiswa.title"));
        lblSubtitle.setText(I18nManager.get("mahasiswa.subtitle"));
        lblPortLabel.setText(I18nManager.get("mahasiswa.portLabel"));
        lblRfidLabel.setText(I18nManager.get("mahasiswa.rfidLabel"));
        btnScan.setText(I18nManager.get("mahasiswa.buttonScan"));
        btnSimulasi.setText(I18nManager.get("mahasiswa.buttonSimulation"));
        btnAdminLogin.setText(I18nManager.get("mahasiswa.buttonAdminLogin"));
        lblNamaLabel.setText(I18nManager.get("mahasiswa.namaLabel"));
        lblNimLabel.setText(I18nManager.get("mahasiswa.nimLabel"));
        lblJurusanLabel.setText(I18nManager.get("mahasiswa.jurusanLabel"));
        lblIdLabel.setText(I18nManager.get("mahasiswa.idLabel"));
        lblLogTitle.setText(I18nManager.get("mahasiswa.logTitle"));
    }

    /**
     * Event handler untuk tombol ID.
     */
    private void btnLanguageIDActionPerformed(java.awt.event.ActionEvent evt) {
        I18nManager.setLocale(new Locale("id", "ID"));
        refreshUITextMahasiswa();
    }

    /**
     * Event handler untuk tombol EN.
     */
    private void btnLanguageENActionPerformed(java.awt.event.ActionEvent evt) {
        I18nManager.setLocale(new Locale("en", "US"));
        refreshUITextMahasiswa();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdminLogin;
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnLanguageEN;
    private javax.swing.JButton btnLanguageID;
    private javax.swing.JButton btnScan;
    private javax.swing.JButton btnSimulasi;
    private javax.swing.JComboBox cbPort;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblClock;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblIdLabel;
    private javax.swing.JLabel lblIdValue;
    private javax.swing.JLabel lblJurusanLabel;
    private javax.swing.JLabel lblJurusanValue;
    private javax.swing.JLabel lblLogTitle;
    private javax.swing.JLabel lblNamaLabel;
    private javax.swing.JLabel lblNamaValue;
    private javax.swing.JLabel lblNimLabel;
    private javax.swing.JLabel lblNimValue;
    private javax.swing.JLabel lblPortLabel;
    private javax.swing.JLabel lblPortStatus;
    private javax.swing.JLabel lblRfidLabel;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblStatusTime;
    private javax.swing.JLabel lblSubtitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JPanel panelInfo;
    private javax.swing.JTable tblLog;
    private javax.swing.JTextField txtRfidInput;
    // End of variables declaration//GEN-END:variables
}
