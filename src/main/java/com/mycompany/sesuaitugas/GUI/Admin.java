package com.mycompany.sesuaitugas.gui;

import com.mycompany.sesuaitugas.objects.DaftarJurusan;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.objects.User;
import com.mycompany.sesuaitugas.services.AbsensiService;
import com.mycompany.sesuaitugas.services.MahasiswaService;
import com.mycompany.sesuaitugas.util.RfidSerialListener;
import com.mycompany.sesuaitugas.util.I18nManager;

import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Admin extends javax.swing.JPanel {
    private static final String SEARCH_PLACEHOLDER = "Search";
    private static final int MAHASISWA_GRID_COLS = 4;
    private static final Dimension MAHASISWA_CARD_SIZE = new Dimension(220, 168);
    private User sessionUser;
    private JLabel jLabelSessionBanner;
    private final MahasiswaService mahasiswaService = new MahasiswaService();
    private final AbsensiService absensiService = new AbsensiService();
    private JPanel mahasiswaListPanel;
    private String selectedGridNim;
    private String editingNimOriginal;
    private List<Mahasiswa> mahasiswaCache = new ArrayList<>();
    private RfidSerialListener rfidRegistrasiListener;

    public Admin() { this(null); }

    public Admin(User sessionUser) {
        this.sessionUser = sessionUser;
        initComponents();
        installSessionBanner();
        addLanguageButtonsAdmin();
        CB_Jurusan.setModel(new DefaultComboBoxModel<>(DaftarJurusan.PILIHAN));
        setupMahasiswaGrid();
        setupSearchField();
        populateRfidPortCombo();
        wireActions();
        reloadMahasiswaFromDb();
        refreshUITextAdmin();
    }

    private void populateRfidPortCombo() {
        try {
            String[] ports = RfidSerialListener.getAvailablePorts();
            if (ports.length == 0) {
                cbRfidPort.setModel(new DefaultComboBoxModel<>(new String[]{"(Tidak ada port)"}));
                btnRfidConnect.setEnabled(false);
            } else {
                cbRfidPort.setModel(new DefaultComboBoxModel<>(ports));
            }
        } catch (Throwable t) {
            cbRfidPort.setModel(new DefaultComboBoxModel<>(new String[]{"(Library tidak tersedia)"}));
            btnRfidConnect.setEnabled(false);
        }
    }

    private void installSessionBanner() {
        jLabelSessionBanner = new JLabel();
        jLabelSessionBanner.setFont(jLabelSessionBanner.getFont().deriveFont(Font.PLAIN, 14f));
        jLabelSessionBanner.setForeground(new Color(25, 55, 95));
        if (sessionUser != null) {
            String roleDb = sessionUser.getRole() != null ? sessionUser.getRole().trim() : "";
            String roleTampil = formatRoleTampilan(roleDb);
            String nama = sessionUser.getNama() != null && !sessionUser.getNama().trim().isEmpty()
                    ? sessionUser.getNama().trim() : sessionUser.getEmail();
            String nimTxt = sessionUser.getNim() != null && !sessionUser.getNim().trim().isEmpty()
                    ? " Â· NIM " + sessionUser.getNim().trim() : "";
            jLabelSessionBanner.setText("Dashboard Â· " + nama + " (" + sessionUser.getEmail() + ")" + nimTxt
                    + "  |  Role (database): " + (roleDb.isEmpty() ? "-" : roleDb) + " (" + roleTampil + ")");
        } else {
            jLabelSessionBanner.setText("Dashboard Â· Role: - (belum ada data sesi login)");
        }
        jPanel1.add(jLabelSessionBanner, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 6, 1400, 30));
    }

    private static String formatRoleTampilan(String roleDb) {
        if (roleDb == null || roleDb.isEmpty()) return "-";
        String r = roleDb.toLowerCase();
        if ("admin".equals(r)) return "Administrator";
        if ("user".equals(r)) return "Mahasiswa";
        return roleDb;
    }

    private void setupMahasiswaGrid() {
        mahasiswaListPanel = jPanelMahasiswaList;
        mahasiswaListPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 14, 12));
        jScrollPaneMahasiswa.setBorder(BorderFactory.createTitledBorder("Mahasiswa terdaftar"));
        jScrollPaneMahasiswa.getVerticalScrollBar().setUnitIncrement(20);
        jScrollPaneMahasiswa.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private JPanel buildMahasiswaCard(Mahasiswa m) {
        final String nim = nullToEmpty(m.getNim());
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.putClientProperty("nim", nim);
        card.setPreferredSize(MAHASISWA_CARD_SIZE);
        card.setMaximumSize(MAHASISWA_CARD_SIZE);
        card.setMinimumSize(MAHASISWA_CARD_SIZE);
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 232), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel nameLb = new JLabel(nullToEmpty(m.getNama()).isEmpty() ? "(Tanpa nama)" : nullToEmpty(m.getNama()));
        nameLb.setFont(nameLb.getFont().deriveFont(Font.BOLD, 14f));
        nameLb.setForeground(new Color(33, 37, 41));
        card.add(nameLb, BorderLayout.NORTH);
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width:190px;color:#555;font-size:11px'>");
        sb.append("<b>NIM</b> ").append(escapeHtml(nim)).append("<br/>");
        sb.append("<b>ID</b> ").append(escapeHtml(nullToEmpty(m.getIdMahasiswa()))).append("<br/>");
        sb.append("<b>Jurusan</b> ").append(escapeHtml(nullToEmpty(m.getJurusan()))).append("<br/>");
        sb.append("<b>Email</b> ").append(escapeHtml(nullToEmpty(m.getEmail()))).append("<br/>");
        sb.append("<br/><b>RFID</b> ").append(m.getRfidHash() != null && !m.getRfidHash().isEmpty()
                ? "<span style='color:green'>&#10003; Terdaftar</span>" : "<span style='color:red'>&#10007; Belum</span>");
        sb.append("</body></html>");
        JLabel metaLb = new JLabel(sb.toString());
        metaLb.setVerticalAlignment(JLabel.TOP);
        card.add(metaLb, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        actions.setOpaque(false);
        JButton btnEdit = new JButton("Edit");
        btnEdit.setFont(btnEdit.getFont().deriveFont(11f));
        btnEdit.setToolTipText("Muat data ke form atas, lalu ubah dan tekan Update");
        btnEdit.addActionListener(e -> selectMahasiswaFromGrid(m));
        JButton btnRiwayat = new JButton("Riwayat");
        btnRiwayat.setFont(btnRiwayat.getFont().deriveFont(11f));
        btnRiwayat.setForeground(new Color(0, 102, 204));
        btnRiwayat.setToolTipText("Lihat riwayat absensi mahasiswa ini");
        btnRiwayat.addActionListener(e -> showRiwayatAbsensi(m.getNim(), m.getNama()));
        JButton btnHapus = new JButton("Hapus");
        btnHapus.setFont(btnHapus.getFont().deriveFont(11f));
        btnHapus.addActionListener(e -> hapusMahasiswaByNim(nim));
        actions.add(btnEdit);
        actions.add(btnRiwayat);
        actions.add(btnHapus);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void updateGridCardBorders() {
        if (mahasiswaListPanel == null) return;
        for (Component rowComp : mahasiswaListPanel.getComponents()) {
            if (!(rowComp instanceof JPanel)) continue;
            for (Component c : ((JPanel) rowComp).getComponents()) {
                if (!(c instanceof JPanel)) continue;
                JPanel card = (JPanel) c;
                String n = (String) card.getClientProperty("nim");
                if (n == null) continue;
                boolean sel = selectedGridNim != null && selectedGridNim.equals(n);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(sel ? new Color(0, 102, 204) : new Color(220, 224, 232), sel ? 2 : 1),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            }
        }
    }

    private void selectMahasiswaFromGrid(Mahasiswa m) {
        if (m == null || m.getNim() == null || m.getNim().trim().isEmpty()) return;
        selectedGridNim = m.getNim().trim();
        editingNimOriginal = selectedGridNim;
        txtNIM.setText(selectedGridNim);
        jTextField2.setText(nullToEmpty(m.getNama()));
        txtPassword.setText(nullToEmpty(m.getPassword()));
        txtEmail.setText(nullToEmpty(m.getEmail()));
        setComboJurusan(m.getJurusan());
        updateGridCardBorders();
    }

    private void setupSearchField() {
        jTextField3.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applySearchFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applySearchFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applySearchFilter(); }
        });
        jTextField3.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (SEARCH_PLACEHOLDER.equals(jTextField3.getText())) jTextField3.setText("");
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (jTextField3.getText().trim().isEmpty()) jTextField3.setText(SEARCH_PLACEHOLDER);
            }
        });
    }

    private void wireActions() {
        jButton5.addActionListener(e -> onSaveMahasiswa());
        jButton6.addActionListener(e -> onUpdateMahasiswa());
        jButton7.addActionListener(e -> reloadMahasiswaFromDb());
        btnRfidConnect.addActionListener(e -> onToggleRfidRegistrasi());
        btnDaftarRfid.addActionListener(e -> onDaftarkanRfid());
        btnLogout.addActionListener(e -> onLogout());
    }

    private void onToggleRfidRegistrasi() {
        if (rfidRegistrasiListener != null && rfidRegistrasiListener.isRunning()) {
            rfidRegistrasiListener.stop();
            rfidRegistrasiListener = null;
            btnRfidConnect.setText("Hubungkan");
            lblRfidPortStatus.setText("Tidak terhubung");
            lblRfidPortStatus.setForeground(Color.GRAY);
        } else {
            String port = (String) cbRfidPort.getSelectedItem();
            if (port == null || port.startsWith("(")) {
                JOptionPane.showMessageDialog(this, "Tidak ada COM port tersedia.", "RFID", JOptionPane.WARNING_MESSAGE);
                return;
            }
            rfidRegistrasiListener = new RfidSerialListener(port, uid -> SwingUtilities.invokeLater(() -> {
                txtUidResult.setText(uid);
                lblRfidPortStatus.setText("UID diterima: " + uid);
                lblRfidPortStatus.setForeground(new Color(0, 120, 0));
            }));
            if (rfidRegistrasiListener.start()) {
                btnRfidConnect.setText("Putuskan");
                lblRfidPortStatus.setText("Menunggu tap kartu...");
                lblRfidPortStatus.setForeground(new Color(0, 100, 200));
            } else {
                rfidRegistrasiListener = null;
                lblRfidPortStatus.setText("Gagal membuka " + port);
                lblRfidPortStatus.setForeground(Color.RED);
            }
        }
    }

    private void onDaftarkanRfid() {
        String uid = txtUidResult.getText().trim();
        String nim = txtRfidNim.getText().trim();
        if (uid.isEmpty() || uid.equals("(belum ada scan)")) {
            JOptionPane.showMessageDialog(this, "Tap kartu RFID terlebih dahulu untuk mendapatkan UID.", "Registrasi RFID", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (nim.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan NIM mahasiswa yang akan didaftarkan.", "Registrasi RFID", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new javax.swing.SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return absensiService.daftarkanRfid(nim, uid); }
            @Override protected void done() {
                try {
                    boolean berhasil = get();
                    if (berhasil) {
                        JOptionPane.showMessageDialog(Admin.this, "Kartu RFID berhasil didaftarkan ke NIM " + nim + ".", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
                        txtUidResult.setText("(belum ada scan)");
                        txtRfidNim.setText("");
                        lblRfidPortStatus.setText("Kartu terdaftar. Siap scan berikutnya.");
                        lblRfidPortStatus.setForeground(new Color(0, 120, 0));
                        reloadMahasiswaFromDb();
                    } else {
                        JOptionPane.showMessageDialog(Admin.this, "NIM " + nim + " tidak ditemukan di database.", "Gagal", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Admin.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String searchQueryNormalized() {
        String t = jTextField3.getText().trim();
        if (t.isEmpty() || SEARCH_PLACEHOLDER.equals(t)) return "";
        return t.toLowerCase();
    }

    private boolean rowMatchesSearch(Mahasiswa m, String q) {
        if (q.isEmpty()) return true;
        String nim = m.getNim() != null ? m.getNim().toLowerCase() : "";
        String id = m.getIdMahasiswa() != null ? m.getIdMahasiswa().toLowerCase() : "";
        String nama = m.getNama() != null ? m.getNama().toLowerCase() : "";
        String jur = m.getJurusan() != null ? m.getJurusan().toLowerCase() : "";
        return nim.contains(q) || id.contains(q) || nama.contains(q) || jur.contains(q);
    }

    private void applySearchFilter() {
        String q = searchQueryNormalized();
        if (mahasiswaListPanel == null) return;
        String preserveNim = selectedGridNim;
        mahasiswaListPanel.removeAll();
        List<Mahasiswa> filtered = new ArrayList<>();
        for (Mahasiswa m : mahasiswaCache) {
            if (rowMatchesSearch(m, q)) filtered.add(m);
        }
        if (filtered.isEmpty()) {
            JPanel empty = new JPanel(new FlowLayout(FlowLayout.LEFT));
            empty.setOpaque(false);
            empty.add(new JLabel("Tidak ada mahasiswa yang cocok dengan pencarian."));
            mahasiswaListPanel.add(empty);
        } else {
            for (int i = 0; i < filtered.size(); i += MAHASISWA_GRID_COLS) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 14));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setOpaque(false);
                for (int j = 0; j < MAHASISWA_GRID_COLS && i + j < filtered.size(); j++) {
                    row.add(buildMahasiswaCard(filtered.get(i + j)));
                }
                mahasiswaListPanel.add(row);
            }
        }
        mahasiswaListPanel.add(Box.createVerticalGlue());
        mahasiswaListPanel.revalidate();
        mahasiswaListPanel.repaint();
        if (preserveNim != null) {
            Mahasiswa stillThere = null;
            for (Mahasiswa m : filtered) {
                if (preserveNim.equals(m.getNim())) { stillThere = m; break; }
            }
            if (stillThere != null) selectMahasiswaFromGrid(stillThere);
            else { selectedGridNim = null; editingNimOriginal = null; updateGridCardBorders(); }
        } else {
            updateGridCardBorders();
        }
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private void reloadMahasiswaFromDb() {
        try {
            mahasiswaCache = new ArrayList<>(mahasiswaService.findAll());
            applySearchFilter();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat data mahasiswa: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setComboJurusan(String jurusan) {
        if (jurusan == null || jurusan.isEmpty()) { CB_Jurusan.setSelectedIndex(0); return; }
        for (int i = 0; i < CB_Jurusan.getItemCount(); i++) {
            if (jurusan.equals(CB_Jurusan.getItemAt(i))) { CB_Jurusan.setSelectedIndex(i); return; }
        }
        CB_Jurusan.setSelectedIndex(0);
    }

    private void clearFormMahasiswa() {
        txtNIM.setText(""); jTextField2.setText(""); txtPassword.setText(""); txtEmail.setText("");
        if (CB_Jurusan.getItemCount() > 0) CB_Jurusan.setSelectedIndex(0);
        selectedGridNim = null; editingNimOriginal = null;
        updateGridCardBorders();
    }

    private Mahasiswa readMahasiswaFromForm() {
        String nim = txtNIM.getText().trim();
        String nama = jTextField2.getText().trim();
        String password = txtPassword.getText().trim();
        String email = txtEmail.getText().trim();
        Object jurObj = CB_Jurusan.getSelectedItem();
        String jurusan = jurObj != null ? jurObj.toString() : "";
        return new Mahasiswa(nim, "", nama, jurusan, password, email);
    }

    private void onSaveMahasiswa() {
        Mahasiswa m = readMahasiswaFromForm();
        if (m.getNim().isEmpty()) { JOptionPane.showMessageDialog(this, "NIM wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE); return; }
        if (!DaftarJurusan.isValidSelection(m.getJurusan())) { JOptionPane.showMessageDialog(this, "Pilih jurusan yang valid.", "Validasi", JOptionPane.WARNING_MESSAGE); return; }
        m.setJurusan(DaftarJurusan.toCanonical(m.getJurusan()));
        try {
            if (mahasiswaService.findByNim(m.getNim()) != null) {
                JOptionPane.showMessageDialog(this, "NIM sudah terdaftar. Gunakan Update atau NIM lain.", "Duplikat", JOptionPane.WARNING_MESSAGE);
                return;
            }
            mahasiswaService.save(m);
            JOptionPane.showMessageDialog(this, "Data mahasiswa disimpan.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            reloadMahasiswaFromDb(); clearFormMahasiswa();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdateMahasiswa() {
        if (editingNimOriginal == null || editingNimOriginal.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Klik Edit pada kartu mahasiswa untuk memuat data ke form, lalu tekan Update setelah mengubah. Untuk data baru gunakan Add.", "Update", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Mahasiswa m = readMahasiswaFromForm();
        if (m.getNim().isEmpty()) { JOptionPane.showMessageDialog(this, "NIM wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE); return; }
        if (!DaftarJurusan.isValidSelection(m.getJurusan())) { JOptionPane.showMessageDialog(this, "Pilih jurusan yang valid.", "Validasi", JOptionPane.WARNING_MESSAGE); return; }
        m.setJurusan(DaftarJurusan.toCanonical(m.getJurusan()));
        try {
            if (!m.getNim().equals(editingNimOriginal) && mahasiswaService.findByNim(m.getNim()) != null) {
                JOptionPane.showMessageDialog(this, "NIM baru sudah dipakai mahasiswa lain.", "Duplikat", JOptionPane.WARNING_MESSAGE);
                return;
            }
            mahasiswaService.updateByNim(editingNimOriginal, m);
            JOptionPane.showMessageDialog(this, "Data mahasiswa diperbarui.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            editingNimOriginal = m.getNim();
            reloadMahasiswaFromDb(); selectRowByNim(m.getNim());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memperbarui: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectRowByNim(String nim) {
        if (nim == null || mahasiswaListPanel == null) return;
        for (Component rowComp : mahasiswaListPanel.getComponents()) {
            if (!(rowComp instanceof JPanel)) continue;
            for (Component c : ((JPanel) rowComp).getComponents()) {
                if (!(c instanceof JPanel)) continue;
                JPanel card = (JPanel) c;
                String n = (String) card.getClientProperty("nim");
                if (n == null) continue;
                if (nim.equals(n)) {
                    Mahasiswa m = mahasiswaService.findByNim(nim);
                    if (m != null) { selectMahasiswaFromGrid(m); card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight())); }
                    return;
                }
            }
        }
    }

    private void hapusMahasiswaByNim(String nim) {
        if (nim == null || nim.isEmpty()) return;
        int ok = JOptionPane.showConfirmDialog(this, "Hapus mahasiswa dengan NIM " + nim + "?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            mahasiswaService.deleteByNim(nim);
            reloadMahasiswaFromDb(); clearFormMahasiswa();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menghapus: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Riwayat absensi per mahasiswa ───────────────────────────────────────────

    private void showRiwayatAbsensi(String nim, String nama) {
        String title = "Riwayat Absensi - " + (nama != null ? nama : nim);
        try {
            // Load student's RSA public key for signature verification
            com.mycompany.sesuaitugas.objects.Mahasiswa mhs =
                    new com.mycompany.sesuaitugas.services.MahasiswaService().findByNim(nim);
            java.security.PublicKey pubKey = null;
            if (mhs != null && mhs.getRsaPublicKey() != null && !mhs.getRsaPublicKey().isEmpty()) {
                try {
                    pubKey = com.mycompany.sesuaitugas.util.CryptoUtil.stringToPublicKey(mhs.getRsaPublicKey());
                } catch (Exception e) { /* public key format error */ }
            }

            java.util.List<com.mycompany.sesuaitugas.objects.Absensi> list =
                    absensiService.findByNim(nim);
            String[][] data = new String[list.size()][5];
            for (int i = 0; i < list.size(); i++) {
                com.mycompany.sesuaitugas.objects.Absensi a = list.get(i);
                data[i][0] = a.getTanggal();
                data[i][1] = a.getWaktu();
                data[i][2] = a.getNama();
                data[i][3] = a.getStatus();

                // Verify signature
                if (a.getSignature() != null && !a.getSignature().isEmpty() && pubKey != null) {
                    String checkData = a.getNim() + "|" + a.getTanggal() + "|" + a.getWaktu();
                    boolean valid = com.mycompany.sesuaitugas.util.CryptoUtil.rsaVerify(checkData, a.getSignature(), pubKey);
                    data[i][4] = valid ? "\u2713 Sah (RSA)" : "\u2717 Invalid";
                } else if (a.getSignature() == null || a.getSignature().isEmpty()) {
                    data[i][4] = "-";
                } else {
                    data[i][4] = "? No Key";
                }
            }
            String[] cols = {"Tanggal", "Waktu", "Nama", "Status", "Signature"};
            javax.swing.JTable table = new javax.swing.JTable(data, cols);
            table.setFont(new java.awt.Font("Segoe UI", 0, 14));
            table.setRowHeight(28);
            table.getTableHeader().setFont(new java.awt.Font("Segoe UI", 1, 14));
            table.getColumnModel().getColumn(3).setPreferredWidth(60);
            table.getColumnModel().getColumn(4).setPreferredWidth(100);
            table.setEnabled(false);
            javax.swing.JScrollPane sp = new javax.swing.JScrollPane(table);
            sp.setPreferredSize(new java.awt.Dimension(700, 350));
            JOptionPane.showMessageDialog(this, sp, title, JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal memuat riwayat: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Logout ────────────────────────────────────────────────────────────────

    private void onLogout() {
        if (rfidRegistrasiListener != null) {
            rfidRegistrasiListener.stop();
            rfidRegistrasiListener = null;
        }
        java.awt.Window parent = SwingUtilities.getWindowAncestor(this);
        if (parent != null) parent.dispose();
        new Login().setVisible(true);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtNIM = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        CB_Jurusan = new javax.swing.JComboBox<>();
        jSeparator1 = new javax.swing.JSeparator();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jTextField3 = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jScrollPaneMahasiswa = new javax.swing.JScrollPane();
        jPanelMahasiswaList = new javax.swing.JPanel();
        jLabelMahasiswaGridHint = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabelPassword = new javax.swing.JLabel();
        txtPassword = new javax.swing.JTextField();
        jLabelEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        jLabelRfidPort = new javax.swing.JLabel();
        cbRfidPort = new javax.swing.JComboBox<>();
        btnRfidConnect = new javax.swing.JButton();
        lblRfidPortStatus = new javax.swing.JLabel();
        jLabelRfidUid = new javax.swing.JLabel();
        txtUidResult = new javax.swing.JTextField();
        jLabelRfidNim = new javax.swing.JLabel();
        txtRfidNim = new javax.swing.JTextField();
        btnDaftarRfid = new javax.swing.JButton();
        btnLanguageID = new javax.swing.JButton();
        btnLanguageEN = new javax.swing.JButton();
        btnLogout = new javax.swing.JButton();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("NIM");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 52, 90, 30));
        jPanel1.add(txtNIM, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 52, 220, 30));

        jLabel3.setText("Nama Mahasiswa");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 52, -1, 30));
        jPanel1.add(jTextField2, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 52, 220, 30));

        CB_Jurusan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "---" }));
        jPanel1.add(CB_Jurusan, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 110, 220, 40));
        jPanel1.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 172, 1700, -1));

        jButton5.setBackground(new java.awt.Color(0, 51, 255));
        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("Add");
        jPanel1.add(jButton5, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 50, 130, -1));

        jButton6.setBackground(new java.awt.Color(204, 204, 204));
        jButton6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton6.setForeground(new java.awt.Color(102, 102, 102));
        jButton6.setText("Update");
        jPanel1.add(jButton6, new org.netbeans.lib.awtextra.AbsoluteConstraints(960, 50, 130, 25));

        jButton7.setBackground(new java.awt.Color(0, 204, 0));
        jButton7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton7.setForeground(new java.awt.Color(255, 255, 255));
        jButton7.setText("Refresh");
        jPanel1.add(jButton7, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 80, 270, -1));

        jTextField3.setText("Search");
        jPanel1.add(jTextField3, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 110, 270, 40));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jScrollPaneMahasiswa.setMinimumSize(new java.awt.Dimension(400, 200));

        jPanelMahasiswaList.setBackground(new java.awt.Color(248, 249, 252));
        jPanelMahasiswaList.setLayout(new javax.swing.BoxLayout(jPanelMahasiswaList, javax.swing.BoxLayout.Y_AXIS));

        jLabelMahasiswaGridHint.setText("Grid kartu mahasiswa (Add / Refresh memuat dari database)");
        jPanelMahasiswaList.add(jLabelMahasiswaGridHint);

        jScrollPaneMahasiswa.setViewportView(jPanelMahasiswaList);

        jPanel3.add(jScrollPaneMahasiswa, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 172, 1700, 1028));

        jLabel5.setText("Jurusan ");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, 90, 30));

        jLabelPassword.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelPassword.setText("Password");
        jPanel1.add(jLabelPassword, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 110, 70, 30));

        txtPassword.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jPanel1.add(txtPassword, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 110, 120, 40));

        jLabelEmail.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabelEmail.setText("Email");
        jPanel1.add(jLabelEmail, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 110, 50, 30));

        txtEmail.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jPanel1.add(txtEmail, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 110, 160, 40));

        jLabelRfidPort.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        jLabelRfidPort.setText("COM Port:");
        jPanel1.add(jLabelRfidPort, new org.netbeans.lib.awtextra.AbsoluteConstraints(1120, 30, 85, 28));

        cbRfidPort.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jPanel1.add(cbRfidPort, new org.netbeans.lib.awtextra.AbsoluteConstraints(1200, 30, 120, 28));

        btnRfidConnect.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnRfidConnect.setText("Hubungkan");
        jPanel1.add(btnRfidConnect, new org.netbeans.lib.awtextra.AbsoluteConstraints(1440, 40, 110, 28));

        lblRfidPortStatus.setText("Tidak terhubung");
        jPanel1.add(lblRfidPortStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(1450, 70, 220, 22));

        jLabelRfidUid.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        jLabelRfidUid.setText("UID Terakhir:");
        jPanel1.add(jLabelRfidUid, new org.netbeans.lib.awtextra.AbsoluteConstraints(1120, 80, 90, 28));

        txtUidResult.setEditable(false);
        txtUidResult.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtUidResult.setText("(belum ada scan)");
        jPanel1.add(txtUidResult, new org.netbeans.lib.awtextra.AbsoluteConstraints(1210, 80, 200, 28));

        jLabelRfidNim.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        jLabelRfidNim.setText("NIM Mahasiswa:");
        jPanel1.add(jLabelRfidNim, new org.netbeans.lib.awtextra.AbsoluteConstraints(1120, 120, 100, 28));

        txtRfidNim.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jPanel1.add(txtRfidNim, new org.netbeans.lib.awtextra.AbsoluteConstraints(1230, 120, 130, 28));

        btnDaftarRfid.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnDaftarRfid.setText("Daftarkan Kartu");
        jPanel1.add(btnDaftarRfid, new org.netbeans.lib.awtextra.AbsoluteConstraints(1430, 120, 140, 28));

        btnLanguageID.setBackground(new java.awt.Color(240, 240, 224));
        btnLanguageID.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        btnLanguageID.setText("🇮🇩 ID");
        btnLanguageID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLanguageIDActionPerformed(evt);
            }
        });
        jPanel1.add(btnLanguageID, new org.netbeans.lib.awtextra.AbsoluteConstraints(1590, 80, 85, 32));

        btnLanguageEN.setBackground(new java.awt.Color(240, 240, 224));
        btnLanguageEN.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        btnLanguageEN.setText("🇬🇧 EN");
        btnLanguageEN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLanguageENActionPerformed(evt);
            }
        });
        jPanel1.add(btnLanguageEN, new org.netbeans.lib.awtextra.AbsoluteConstraints(1590, 120, 85, 32));

        btnLogout.setBackground(new java.awt.Color(204, 51, 51));
        btnLogout.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLogout.setForeground(new java.awt.Color(255, 255, 255));
        btnLogout.setText("Logout");
        jPanel1.add(btnLogout, new org.netbeans.lib.awtextra.AbsoluteConstraints(1580, 30, 100, 32));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(89, 89, 89)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(303, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 1200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Tambah tombol ganti bahasa (ID/EN) di panel admin.
     */
    private void addLanguageButtonsAdmin() {
        JPanel langPanel = new JPanel();
        langPanel.setBackground(new Color(240, 248, 250));
        langPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        
        JLabel langLabel = new JLabel("Bahasa / Language:");
        langLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        JButton btnID = new JButton("🇮🇩 ID");
        btnID.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnID.addActionListener(e -> {
            I18nManager.setLocale(new Locale("id", "ID"));
            refreshUITextAdmin();
        });
        
        JButton btnEN = new JButton("🇬🇧 EN");
        btnEN.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnEN.addActionListener(e -> {
            I18nManager.setLocale(new Locale("en", "US"));
            refreshUITextAdmin();
        });
        
        langPanel.add(langLabel);
        langPanel.add(btnID);
        langPanel.add(btnEN);
        
        jPanel1.add(langPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(1300, 50, 280, 28));
    }

    /**
     * Refresh semua text UI di Admin sesuai locale.
     */
    private void refreshUITextAdmin() {
        jLabel1.setText(I18nManager.get("admin.title"));
        jLabel3.setText(I18nManager.get("admin.subtitle"));
        jButton5.setText(I18nManager.get("admin.buttonAdd"));
        jButton6.setText(I18nManager.get("admin.buttonEdit"));
        jButton7.setText(I18nManager.get("admin.buttonDelete"));
        btnLogout.setText(I18nManager.get("admin.buttonLogout"));
        jLabelRfidPort.setText(I18nManager.get("admin.portLabel"));
        btnRfidConnect.setText(I18nManager.get("admin.buttonConnect"));
        btnDaftarRfid.setText(I18nManager.get("admin.buttonRegisterCard"));
        jLabelRfidUid.setText(I18nManager.get("admin.uidResult"));
        jLabelRfidNim.setText(I18nManager.get("admin.nimToRegister"));
        jLabelEmail.setText(I18nManager.get("admin.email"));
        jLabelPassword.setText(I18nManager.get("admin.email"));
    }

    /**
     * Event handler untuk tombol ID.
     */
    private void btnLanguageIDActionPerformed(java.awt.event.ActionEvent evt) {
        I18nManager.setLocale(new Locale("id", "ID"));
        refreshUITextAdmin();
    }

    /**
     * Event handler untuk tombol EN.
     */
    private void btnLanguageENActionPerformed(java.awt.event.ActionEvent evt) {
        I18nManager.setLocale(new Locale("en", "US"));
        refreshUITextAdmin();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> CB_Jurusan;
    private javax.swing.JButton btnDaftarRfid;
    private javax.swing.JButton btnLanguageEN;
    private javax.swing.JButton btnLanguageID;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnRfidConnect;
    private javax.swing.JComboBox<String> cbRfidPort;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabelEmail;
    private javax.swing.JLabel jLabelMahasiswaGridHint;
    private javax.swing.JLabel jLabelPassword;
    private javax.swing.JLabel jLabelRfidNim;
    private javax.swing.JLabel jLabelRfidPort;
    private javax.swing.JLabel jLabelRfidUid;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelMahasiswaList;
    private javax.swing.JScrollPane jScrollPaneMahasiswa;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JLabel lblRfidPortStatus;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtNIM;
    private javax.swing.JTextField txtPassword;
    private javax.swing.JTextField txtRfidNim;
    private javax.swing.JTextField txtUidResult;
    // End of variables declaration//GEN-END:variables
}