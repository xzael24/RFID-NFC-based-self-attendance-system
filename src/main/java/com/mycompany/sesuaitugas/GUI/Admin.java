/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.mycompany.sesuaitugas.gui;

import com.mycompany.sesuaitugas.objects.DaftarJurusan;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.objects.MahasiswaService;
import com.mycompany.sesuaitugas.objects.User;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
/**
 *
 * @author ASUS
 */
public class Admin extends javax.swing.JPanel {
    private static final String SEARCH_PLACEHOLDER = "Search";
    private static final int MAHASISWA_GRID_COLS = 4;
    private static final Dimension MAHASISWA_CARD_SIZE = new Dimension(220, 168);
    /** Pengguna yang sedang login; boleh null. */
    private User sessionUser;
    private JLabel jLabelSessionBanner;
    private final MahasiswaService mahasiswaService = new MahasiswaService();
    /** Panel vertikal berisi baris-baris kartu mahasiswa (di dalam scroll). */
    private JPanel mahasiswaListPanel;
    /** NIM kartu yang sedang dipilih di grid (sinkron dengan form). */
    private String selectedGridNim;
    /** NIM baris yang sedang diedit (null jika form kosong / baru). */
    private String editingNimOriginal;
    private List<Mahasiswa> mahasiswaCache = new ArrayList<>();
    /**
     * Creates new form Admin (tanpa info sesi).
     */
    public Admin() {
        this(null);
    }

    /**
     * @param sessionUser pengguna yang login; dipakai untuk teks role di dashboard.
     */
    public Admin(User sessionUser) {
        this.sessionUser = sessionUser;
        initComponents();
        installSessionBanner();
        CB_Jurusan.setModel(new DefaultComboBoxModel<>(DaftarJurusan.PILIHAN));
        setupMahasiswaGrid();
        setupSearchField();
        wireActions();
        reloadMahasiswaFromDb();
    }

    private void installSessionBanner() {
        jLabelSessionBanner = new JLabel();
        jLabelSessionBanner.setFont(jLabelSessionBanner.getFont().deriveFont(Font.PLAIN, 14f));
        jLabelSessionBanner.setForeground(new Color(25, 55, 95));
        if (sessionUser != null) {
            String roleDb = sessionUser.getRole() != null ? sessionUser.getRole().trim() : "";
            String roleTampil = formatRoleTampilan(roleDb);
            String nama = sessionUser.getNama() != null && !sessionUser.getNama().trim().isEmpty()
                    ? sessionUser.getNama().trim()
                    : sessionUser.getEmail();
            String nimTxt = sessionUser.getNim() != null && !sessionUser.getNim().trim().isEmpty()
                    ? " · NIM " + sessionUser.getNim().trim()
                    : "";
            jLabelSessionBanner.setText("Dashboard · " + nama + " (" + sessionUser.getEmail() + ")" + nimTxt
                    + "  |  Role (database): " + (roleDb.isEmpty() ? "-" : roleDb)
                    + " (" + roleTampil + ")");
        } else {
            jLabelSessionBanner.setText("Dashboard · Role: — (belum ada data sesi login)");
        }
        jPanel1.add(jLabelSessionBanner, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 6, 1400, 30));
    }

    private static String formatRoleTampilan(String roleDb) {
        if (roleDb == null || roleDb.isEmpty()) {
            return "-";
        }
        String r = roleDb.toLowerCase();
        if ("admin".equals(r)) {
            return "Administrator";
        }
        if ("user".equals(r)) {
            return "Mahasiswa";
        }
        return roleDb;
    }

    /**
     * Panel daftar kartu ada di {@code Admin.form} ({@link #jPanelMahasiswaList}) supaya tampil di NetBeans Design.
     */
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
        sb.append("<b>Jurusan</b> ").append(escapeHtml(nullToEmpty(m.getJurusan())));
        sb.append("</body></html>");
        JLabel metaLb = new JLabel(sb.toString());
        metaLb.setVerticalAlignment(JLabel.TOP);
        card.add(metaLb, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        actions.setOpaque(false);
        JButton btnEdit = new JButton("Edit");
        btnEdit.setFont(btnEdit.getFont().deriveFont(11f));
        btnEdit.setToolTipText("Muat data ke form atas, lalu ubah dan tekan Update");
        btnEdit.addActionListener(e -> selectMahasiswaFromGrid(m));
        JButton btnHapus = new JButton("Hapus");
        btnHapus.setFont(btnHapus.getFont().deriveFont(11f));
        btnHapus.addActionListener(e -> hapusMahasiswaByNim(nim));
        actions.add(btnEdit);
        actions.add(btnHapus);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void updateGridCardBorders() {
        if (mahasiswaListPanel == null) {
            return;
        }
        for (Component rowComp : mahasiswaListPanel.getComponents()) {
            if (!(rowComp instanceof JPanel)) {
                continue;
            }
            for (Component c : ((JPanel) rowComp).getComponents()) {
                if (!(c instanceof JPanel)) {
                    continue;
                }
                JPanel card = (JPanel) c;
                String n = (String) card.getClientProperty("nim");
                if (n == null) {
                    continue;
                }
                boolean sel = selectedGridNim != null && selectedGridNim.equals(n);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(sel ? new Color(0, 102, 204) : new Color(220, 224, 232), sel ? 2 : 1),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            }
        }
    }

    private void selectMahasiswaFromGrid(Mahasiswa m) {
        if (m == null || m.getNim() == null || m.getNim().trim().isEmpty()) {
            return;
        }
        selectedGridNim = m.getNim().trim();
        editingNimOriginal = selectedGridNim;
        txtNIM.setText(selectedGridNim);
        jTextField2.setText(nullToEmpty(m.getNama()));
        setComboJurusan(m.getJurusan());
        updateGridCardBorders();
    }
    private void setupSearchField() {
        jTextField3.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySearchFilter();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                applySearchFilter();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                applySearchFilter();
            }
        });
        jTextField3.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (SEARCH_PLACEHOLDER.equals(jTextField3.getText())) {
                    jTextField3.setText("");
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (jTextField3.getText().trim().isEmpty()) {
                    jTextField3.setText(SEARCH_PLACEHOLDER);
                }
            }
        });
    }
    private void wireActions() {
        jButton5.addActionListener(e -> onSaveMahasiswa());
        jButton6.addActionListener(e -> onUpdateMahasiswa());
        jButton7.addActionListener(e -> reloadMahasiswaFromDb());
    }

    private String searchQueryNormalized() {
        String t = jTextField3.getText().trim();
        if (t.isEmpty() || SEARCH_PLACEHOLDER.equals(t)) {
            return "";
        }
        return t.toLowerCase();
    }
    private boolean rowMatchesSearch(Mahasiswa m, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String nim = m.getNim() != null ? m.getNim().toLowerCase() : "";
        String id = m.getIdMahasiswa() != null ? m.getIdMahasiswa().toLowerCase() : "";
        String nama = m.getNama() != null ? m.getNama().toLowerCase() : "";
        String jur = m.getJurusan() != null ? m.getJurusan().toLowerCase() : "";
        return nim.contains(q) || id.contains(q) || nama.contains(q) || jur.contains(q);
    }
    private void applySearchFilter() {
        String q = searchQueryNormalized();
        if (mahasiswaListPanel == null) {
            return;
        }
        String preserveNim = selectedGridNim;
        mahasiswaListPanel.removeAll();
        List<Mahasiswa> filtered = new ArrayList<>();
        for (Mahasiswa m : mahasiswaCache) {
            if (rowMatchesSearch(m, q)) {
                filtered.add(m);
            }
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
                if (preserveNim.equals(m.getNim())) {
                    stillThere = m;
                    break;
                }
            }
            if (stillThere != null) {
                selectMahasiswaFromGrid(stillThere);
            } else {
                selectedGridNim = null;
                editingNimOriginal = null;
                updateGridCardBorders();
            }
        } else {
            updateGridCardBorders();
        }
    }
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
    private void reloadMahasiswaFromDb() {
        try {
            // Muat data hanya dari koleksi 'mahasiswa'
            mahasiswaCache = new ArrayList<>(mahasiswaService.findAll());
            applySearchFilter();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Gagal memuat data mahasiswa: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    private void setComboJurusan(String jurusan) {
        if (jurusan == null || jurusan.isEmpty()) {
            CB_Jurusan.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < CB_Jurusan.getItemCount(); i++) {
            if (jurusan.equals(CB_Jurusan.getItemAt(i))) {
                CB_Jurusan.setSelectedIndex(i);
                return;
            }
        }
        CB_Jurusan.setSelectedIndex(0);
    }
    private void clearFormMahasiswa() {
        txtNIM.setText("");
        jTextField2.setText("");
        if (CB_Jurusan.getItemCount() > 0) {
            CB_Jurusan.setSelectedIndex(0);
        }
        selectedGridNim = null;
        editingNimOriginal = null;
        updateGridCardBorders();
    }
    private Mahasiswa readMahasiswaFromForm() {
        String nim = txtNIM.getText().trim();
        String nama = jTextField2.getText().trim();
        Object jurObj = CB_Jurusan.getSelectedItem();
        String jurusan = jurObj != null ? jurObj.toString() : "";
        return new Mahasiswa(nim, "", nama, jurusan);
    }
    private void onSaveMahasiswa() {
        Mahasiswa m = readMahasiswaFromForm();
        if (m.getNim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "NIM wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!DaftarJurusan.isValidSelection(m.getJurusan())) {
            JOptionPane.showMessageDialog(this, "Pilih jurusan yang valid.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        m.setJurusan(DaftarJurusan.toCanonical(m.getJurusan()));
        try {
            if (mahasiswaService.findByNim(m.getNim()) != null) {
                JOptionPane.showMessageDialog(this,
                        "NIM sudah terdaftar. Gunakan Update atau NIM lain.",
                        "Duplikat",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            mahasiswaService.save(m);
            JOptionPane.showMessageDialog(this, "Data mahasiswa disimpan.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            reloadMahasiswaFromDb();
            clearFormMahasiswa();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void onUpdateMahasiswa() {
        if (editingNimOriginal == null || editingNimOriginal.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Klik Edit pada kartu mahasiswa untuk memuat data ke form, lalu tekan Update setelah mengubah. Untuk data baru gunakan Add.",
                    "Update",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Mahasiswa m = readMahasiswaFromForm();
        if (m.getNim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "NIM wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!DaftarJurusan.isValidSelection(m.getJurusan())) {
            JOptionPane.showMessageDialog(this, "Pilih jurusan yang valid.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        m.setJurusan(DaftarJurusan.toCanonical(m.getJurusan()));
        try {
            if (!m.getNim().equals(editingNimOriginal) && mahasiswaService.findByNim(m.getNim()) != null) {
                JOptionPane.showMessageDialog(this, "NIM baru sudah dipakai mahasiswa lain.", "Duplikat", JOptionPane.WARNING_MESSAGE);
                return;
            }
            mahasiswaService.updateByNim(editingNimOriginal, m);
            JOptionPane.showMessageDialog(this, "Data mahasiswa diperbarui.", "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            editingNimOriginal = m.getNim();
            reloadMahasiswaFromDb();
            selectRowByNim(m.getNim());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memperbarui: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void selectRowByNim(String nim) {
        if (nim == null || mahasiswaListPanel == null) {
            return;
        }
        for (Component rowComp : mahasiswaListPanel.getComponents()) {
            if (!(rowComp instanceof JPanel)) {
                continue;
            }
            for (Component c : ((JPanel) rowComp).getComponents()) {
                if (!(c instanceof JPanel)) {
                    continue;
                }
                JPanel card = (JPanel) c;
                String n = (String) card.getClientProperty("nim");
                if (n == null) {
                    continue;
                }
                if (nim.equals(n)) {
                    Mahasiswa m = mahasiswaService.findByNim(nim);
                    if (m != null) {
                        selectMahasiswaFromGrid(m);
                        card.scrollRectToVisible(new Rectangle(0, 0, card.getWidth(), card.getHeight()));
                    }
                    return;
                }
            }
        }
    }
    private void hapusMahasiswaByNim(String nim) {
        if (nim == null || nim.isEmpty()) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Hapus mahasiswa dengan NIM " + nim + "?",
                "Konfirmasi",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            mahasiswaService.deleteByNim(nim);
            reloadMahasiswaFromDb();
            clearFormMahasiswa();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menghapus: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
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
        jPanel1.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 172, 1830, -1));

        jButton5.setBackground(new java.awt.Color(0, 51, 255));
        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("Add");
        jPanel1.add(jButton5, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 52, 130, -1));

        jButton6.setBackground(new java.awt.Color(204, 204, 204));
        jButton6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton6.setForeground(new java.awt.Color(102, 102, 102));
        jButton6.setText("Update");
        jPanel1.add(jButton6, new org.netbeans.lib.awtextra.AbsoluteConstraints(980, 52, 130, 25));

        jButton7.setBackground(new java.awt.Color(0, 204, 0));
        jButton7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton7.setForeground(new java.awt.Color(255, 255, 255));
        jButton7.setText("Refresh");
        jPanel1.add(jButton7, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 90, 270, -1));

        jTextField3.setText("Search");
        jPanel1.add(jTextField3, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 110, 350, 40));

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jScrollPaneMahasiswa.setMinimumSize(new java.awt.Dimension(400, 200));

        jPanelMahasiswaList.setBackground(new java.awt.Color(248, 249, 252));
        jPanelMahasiswaList.setLayout(new javax.swing.BoxLayout(jPanelMahasiswaList, javax.swing.BoxLayout.Y_AXIS));

        jLabelMahasiswaGridHint.setText("Grid kartu mahasiswa (Add / Refresh memuat dari database)");
        jLabelMahasiswaGridHint.setAlignmentX(0.0F);
        jPanelMahasiswaList.add(jLabelMahasiswaGridHint);

        jScrollPaneMahasiswa.setViewportView(jPanelMahasiswaList);
        jPanel3.add(jScrollPaneMahasiswa, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 172, 1870, 1028));

        jLabel5.setText("Jurusan ");
        jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, 90, 30));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(246, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 1200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> CB_Jurusan;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelMahasiswaList;
    private javax.swing.JScrollPane jScrollPaneMahasiswa;
    private javax.swing.JLabel jLabelMahasiswaGridHint;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField txtNIM;
    // End of variables declaration//GEN-END:variables
}
