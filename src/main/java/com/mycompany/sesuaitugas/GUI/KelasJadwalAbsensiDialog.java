package com.mycompany.sesuaitugas.gui;

import com.mycompany.sesuaitugas.objects.JadwalKelas;
import com.mycompany.sesuaitugas.objects.JadwalKelasService;
import com.mycompany.sesuaitugas.objects.Kelas;
import com.mycompany.sesuaitugas.objects.KelasService;
import com.mycompany.sesuaitugas.objects.LogAbsensi;
import com.mycompany.sesuaitugas.objects.LogAbsensiService;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.objects.MahasiswaService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Dialog admin: Kelas, Jadwal per kelas, dan log absensi.
 */
public class KelasJadwalAbsensiDialog extends JDialog {

    private static final String[] HARI_OPTIONS = {
        "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
    };
    private static final String[] STATUS_ABSENSI = {
        "Hadir", "Izin", "Sakit", "Alpha"
    };

    private final KelasService kelasService = new KelasService();
    private final JadwalKelasService jadwalService = new JadwalKelasService();
    private final LogAbsensiService absensiService = new LogAbsensiService();
    private final MahasiswaService mahasiswaService = new MahasiswaService();

    // --- Tab Kelas ---
    private final JTextField kKode = new JTextField(12);
    private final JTextField kNama = new JTextField(20);
    private final JTextField kSks = new JTextField(4);
    private final JTextField kDosen = new JTextField(16);
    private final JTextField kRuangan = new JTextField(10);
    private DefaultTableModel kelasModel;
    private JTable kelasTable;
    private String editingKodeKelas;

    // --- Tab Jadwal ---
    private final JComboBox<String> jFilterKelas = new JComboBox<>();
    private final JComboBox<String> jHari = new JComboBox<>(HARI_OPTIONS);
    private final JTextField jJamMulai = new JTextField(8);
    private final JTextField jJamSelesai = new JTextField(8);
    private final JTextField jRuangan = new JTextField(10);
    private DefaultTableModel jadwalModel;
    private JTable jadwalTable;
    private String editingJadwalId;

    // --- Tab Absensi ---
    private final JComboBox<String> aNim = new JComboBox<>();
    private final JComboBox<String> aKelas = new JComboBox<>();
    private final JComboBox<String> aStatus = new JComboBox<>(STATUS_ABSENSI);
    private final JSpinner aWaktu = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.MINUTE));
    private final JTextField aRfid = new JTextField(12);
    private final JTextField aKet = new JTextField(24);
    private DefaultTableModel absensiModel;
    private JTable absensiTable;

    public KelasJadwalAbsensiDialog(Frame owner) {
        this(owner, 0);
    }

    /**
     * @param initialTabIndex 0 = Kelas, 1 = Jadwal, 2 = Log absensi
     */
    public KelasJadwalAbsensiDialog(Frame owner, int initialTabIndex) {
        super(owner, titleForInitialTab(initialTabIndex), true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Kelas", buildTabKelas());
        tabs.addTab("Jadwal", buildTabJadwal());
        tabs.addTab("Log absensi", buildTabAbsensi());
        int idx = Math.max(0, Math.min(initialTabIndex, tabs.getTabCount() - 1));
        tabs.setSelectedIndex(idx);
        add(tabs, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton tutup = new JButton("Tutup");
        tutup.addActionListener(e -> dispose());
        bottom.add(tutup);
        add(bottom, BorderLayout.SOUTH);
        setSize(920, 560);
        setLocationRelativeTo(owner);
        reloadKelasTable();
        reloadJadwalTable();
        reloadAbsensiTable();
        refreshKelasCombos();
        refreshMahasiswaNimCombo();
    }

    private static String titleForInitialTab(int tab) {
        switch (tab) {
            case 0:
                return "Manage kelas";
            case 1:
                return "Manage jadwal";
            case 2:
                return "Log absensi";
            default:
                return "Kelas & jadwal";
        }
    }

    private JPanel buildTabKelas() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Kode:"));
        form.add(kKode);
        form.add(new JLabel("Nama MK:"));
        form.add(kNama);
        form.add(new JLabel("SKS:"));
        form.add(kSks);
        form.add(new JLabel("Dosen:"));
        form.add(kDosen);
        form.add(new JLabel("Ruangan:"));
        form.add(kRuangan);
        JButton bSave = new JButton("Simpan");
        JButton bUpdate = new JButton("Perbarui");
        JButton bClear = new JButton("Bersihkan");
        JButton bRefresh = new JButton("Segarkan");
        bSave.addActionListener(e -> onKelasSave());
        bUpdate.addActionListener(e -> onKelasUpdate());
        bClear.addActionListener(e -> clearKelasForm());
        bRefresh.addActionListener(e -> {
            reloadKelasTable();
            refreshKelasCombos();
        });
        form.add(bSave);
        form.add(bUpdate);
        form.add(bClear);
        form.add(bRefresh);
        root.add(form, BorderLayout.NORTH);
        String[] cols = {"Kode", "Nama MK", "SKS", "Dosen", "Ruangan"};
        kelasModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        kelasTable = new JTable(kelasModel);
        kelasTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        kelasTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncKelasFromRow();
            }
        });
        JPopupMenu pm = new JPopupMenu();
        javax.swing.JMenuItem miDel = new javax.swing.JMenuItem("Hapus kelas");
        miDel.addActionListener(e -> onKelasDelete());
        pm.add(miDel);
        kelasTable.setComponentPopupMenu(pm);
        root.add(new JScrollPane(kelasTable), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildTabJadwal() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Kelas:"));
        jFilterKelas.setPreferredSize(new Dimension(160, 26));
        form.add(jFilterKelas);
        form.add(new JLabel("Hari:"));
        form.add(jHari);
        form.add(new JLabel("Jam mulai:"));
        jJamMulai.setToolTipText("Contoh: 08:00");
        form.add(jJamMulai);
        form.add(new JLabel("Jam selesai:"));
        form.add(jJamSelesai);
        form.add(new JLabel("Ruangan:"));
        form.add(jRuangan);
        JButton bAdd = new JButton("Tambah jadwal");
        JButton bUpd = new JButton("Perbarui");
        JButton bClr = new JButton("Bersihkan");
        JButton bRef = new JButton("Segarkan");
        bAdd.addActionListener(e -> onJadwalAdd());
        bUpd.addActionListener(e -> onJadwalUpdate());
        bClr.addActionListener(e -> clearJadwalForm());
        bRef.addActionListener(e -> reloadJadwalTable());
        jFilterKelas.addActionListener(e -> reloadJadwalTable());
        form.add(bAdd);
        form.add(bUpd);
        form.add(bClr);
        form.add(bRef);
        root.add(form, BorderLayout.NORTH);
        String[] cols = {"ID", "Kode kelas", "Hari", "Jam mulai", "Jam selesai", "Ruangan"};
        jadwalModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        jadwalTable = new JTable(jadwalModel);
        jadwalTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jadwalTable.getColumnModel().getColumn(0).setMinWidth(0);
        jadwalTable.getColumnModel().getColumn(0).setMaxWidth(0);
        jadwalTable.getColumnModel().getColumn(0).setWidth(0);
        jadwalTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncJadwalFromRow();
            }
        });
        JPopupMenu pm = new JPopupMenu();
        javax.swing.JMenuItem miDel = new javax.swing.JMenuItem("Hapus jadwal");
        miDel.addActionListener(e -> onJadwalDelete());
        pm.add(miDel);
        jadwalTable.setComponentPopupMenu(pm);
        root.add(new JScrollPane(jadwalTable), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildTabAbsensi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        aNim.setEditable(true);
        aNim.setPreferredSize(new Dimension(140, 26));
        aKelas.setPreferredSize(new Dimension(140, 26));
        form.add(new JLabel("NIM:"));
        form.add(aNim);
        form.add(new JLabel("Kelas:"));
        form.add(aKelas);
        form.add(new JLabel("Status:"));
        form.add(aStatus);
        form.add(new JLabel("Waktu:"));
        aWaktu.setPreferredSize(new Dimension(160, 26));
        form.add(aWaktu);
        form.add(new JLabel("UID RFID (opsional):"));
        form.add(aRfid);
        form.add(new JLabel("Keterangan:"));
        form.add(aKet);
        JButton bSave = new JButton("Simpan log");
        JButton bRef = new JButton("Segarkan");
        bSave.addActionListener(e -> onAbsensiSave());
        bRef.addActionListener(e -> {
            reloadAbsensiTable();
            refreshMahasiswaNimCombo();
            refreshKelasCombos();
        });
        form.add(bSave);
        form.add(bRef);
        root.add(form, BorderLayout.NORTH);
        String[] cols = {"ID", "Waktu", "NIM", "Kode kelas", "Status", "RFID", "Keterangan"};
        absensiModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        absensiTable = new JTable(absensiModel);
        absensiTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        absensiTable.getColumnModel().getColumn(0).setMinWidth(0);
        absensiTable.getColumnModel().getColumn(0).setMaxWidth(0);
        absensiTable.getColumnModel().getColumn(0).setWidth(0);
        JPopupMenu pm = new JPopupMenu();
        javax.swing.JMenuItem miDel = new javax.swing.JMenuItem("Hapus log");
        miDel.addActionListener(e -> onAbsensiDelete());
        pm.add(miDel);
        absensiTable.setComponentPopupMenu(pm);
        root.add(new JScrollPane(absensiTable), BorderLayout.CENTER);
        return root;
    }

    private void refreshKelasCombos() {
        String prevJ = jFilterKelas.getSelectedItem() != null ? jFilterKelas.getSelectedItem().toString() : "";
        String prevA = aKelas.getSelectedItem() != null ? aKelas.getSelectedItem().toString() : "";
        java.util.List<String> codes = new java.util.ArrayList<>();
        codes.add("(semua)");
        try {
            for (Kelas k : kelasService.findAll()) {
                if (k.getKodeKelas() != null) {
                    codes.add(k.getKodeKelas());
                }
            }
        } catch (Exception ignored) {
        }
        jFilterKelas.setModel(new DefaultComboBoxModel<>(codes.toArray(new String[0])));
        selectComboItem(jFilterKelas, prevJ);
        java.util.List<String> onlyKelas = new java.util.ArrayList<>();
        for (int i = 1; i < codes.size(); i++) {
            onlyKelas.add(codes.get(i));
        }
        aKelas.setModel(new DefaultComboBoxModel<>(onlyKelas.toArray(new String[0])));
        selectComboItem(aKelas, prevA);
    }

    private static void selectComboItem(JComboBox<String> cb, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (value.equals(cb.getItemAt(i))) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    private void refreshMahasiswaNimCombo() {
        String prev = aNim.getSelectedItem() != null ? aNim.getSelectedItem().toString() : "";
        java.util.List<String> nims = new java.util.ArrayList<>();
        try {
            for (Mahasiswa m : mahasiswaService.findAll()) {
                if (m.getNim() != null && !m.getNim().trim().isEmpty()) {
                    nims.add(m.getNim().trim());
                }
            }
        } catch (Exception ignored) {
        }
        aNim.setModel(new DefaultComboBoxModel<>(nims.toArray(new String[0])));
        if (!prev.isEmpty()) {
            aNim.setSelectedItem(prev);
        }
    }

    // --- Kelas CRUD ---
    private void reloadKelasTable() {
        kelasModel.setRowCount(0);
        try {
            for (Kelas k : kelasService.findAll()) {
                kelasModel.addRow(new Object[]{
                    safe(k.getKodeKelas()),
                    safe(k.getNamaMataKuliah()),
                    k.getSks(),
                    safe(k.getDosen()),
                    safe(k.getRuangan())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat kelas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void syncKelasFromRow() {
        int r = kelasTable.getSelectedRow();
        if (r < 0) {
            editingKodeKelas = null;
            return;
        }
        editingKodeKelas = String.valueOf(kelasModel.getValueAt(r, 0));
        kKode.setText(editingKodeKelas);
        kNama.setText(String.valueOf(kelasModel.getValueAt(r, 1)));
        kSks.setText(String.valueOf(kelasModel.getValueAt(r, 2)));
        kDosen.setText(String.valueOf(kelasModel.getValueAt(r, 3)));
        kRuangan.setText(String.valueOf(kelasModel.getValueAt(r, 4)));
    }

    private void clearKelasForm() {
        kKode.setText("");
        kNama.setText("");
        kSks.setText("");
        kDosen.setText("");
        kRuangan.setText("");
        kelasTable.clearSelection();
        editingKodeKelas = null;
    }

    private void onKelasSave() {
        Kelas k = readKelasFromForm();
        if (k.getKodeKelas().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kode kelas wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (kelasService.findByKode(k.getKodeKelas()) != null) {
                JOptionPane.showMessageDialog(this, "Kode kelas sudah ada. Gunakan Perbarui.", "Duplikat", JOptionPane.WARNING_MESSAGE);
                return;
            }
            kelasService.save(k);
            JOptionPane.showMessageDialog(this, "Kelas disimpan.", "OK", JOptionPane.INFORMATION_MESSAGE);
            reloadKelasTable();
            refreshKelasCombos();
            clearKelasForm();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onKelasUpdate() {
        if (editingKodeKelas == null || editingKodeKelas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih baris di tabel atau isi kode lalu simpan sebagai data baru.", "Perbarui", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Kelas k = readKelasFromForm();
        if (k.getKodeKelas().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kode kelas wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (!k.getKodeKelas().equals(editingKodeKelas) && kelasService.findByKode(k.getKodeKelas()) != null) {
                JOptionPane.showMessageDialog(this, "Kode baru sudah dipakai kelas lain.", "Duplikat", JOptionPane.WARNING_MESSAGE);
                return;
            }
            kelasService.updateByKode(editingKodeKelas, k);
            if (!k.getKodeKelas().equals(editingKodeKelas)) {
                for (JadwalKelas j : jadwalService.findByKodeKelas(editingKodeKelas)) {
                    j.setKodeKelas(k.getKodeKelas());
                    jadwalService.updateById(j.getId(), j);
                }
            }
            JOptionPane.showMessageDialog(this, "Kelas diperbarui.", "OK", JOptionPane.INFORMATION_MESSAGE);
            editingKodeKelas = k.getKodeKelas();
            reloadKelasTable();
            reloadJadwalTable();
            refreshKelasCombos();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onKelasDelete() {
        int r = kelasTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih kelas yang akan dihapus.", "Hapus", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String kode = String.valueOf(kelasModel.getValueAt(r, 0)).trim();
        if (kode.isEmpty()) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Hapus kelas " + kode + " beserta semua jadwalnya? (Log absensi tetap di database.)",
                "Konfirmasi", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            kelasService.deleteByKode(kode);
            reloadKelasTable();
            reloadJadwalTable();
            refreshKelasCombos();
            clearKelasForm();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Kelas readKelasFromForm() {
        int sks = 0;
        try {
            sks = Integer.parseInt(kSks.getText().trim());
        } catch (NumberFormatException ignored) {
        }
        return new Kelas(
                kKode.getText().trim(),
                kNama.getText().trim(),
                sks,
                kDosen.getText().trim(),
                kRuangan.getText().trim());
    }

    // --- Jadwal ---
    private void reloadJadwalTable() {
        jadwalModel.setRowCount(0);
        try {
            Object sel = jFilterKelas.getSelectedItem();
            String filter = sel != null ? sel.toString() : "";
            List<JadwalKelas> list;
            if (filter.isEmpty() || "(semua)".equals(filter)) {
                list = jadwalService.findAll();
            } else {
                list = jadwalService.findByKodeKelas(filter);
            }
            for (JadwalKelas j : list) {
                jadwalModel.addRow(new Object[]{
                    safe(j.getId()),
                    safe(j.getKodeKelas()),
                    safe(j.getHari()),
                    safe(j.getJamMulai()),
                    safe(j.getJamSelesai()),
                    safe(j.getRuangan())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat jadwal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void syncJadwalFromRow() {
        int r = jadwalTable.getSelectedRow();
        if (r < 0) {
            editingJadwalId = null;
            return;
        }
        editingJadwalId = String.valueOf(jadwalModel.getValueAt(r, 0));
        String kode = String.valueOf(jadwalModel.getValueAt(r, 1));
        selectComboItem(jFilterKelas, kode);
        if (jFilterKelas.getSelectedItem() == null || !(kode.equals(String.valueOf(jFilterKelas.getSelectedItem())))) {
            for (int i = 0; i < jFilterKelas.getItemCount(); i++) {
                if (kode.equals(jFilterKelas.getItemAt(i))) {
                    jFilterKelas.setSelectedIndex(i);
                    break;
                }
            }
        }
        String hari = String.valueOf(jadwalModel.getValueAt(r, 2));
        for (int i = 0; i < jHari.getItemCount(); i++) {
            if (hari.equals(jHari.getItemAt(i))) {
                jHari.setSelectedIndex(i);
                break;
            }
        }
        jJamMulai.setText(String.valueOf(jadwalModel.getValueAt(r, 3)));
        jJamSelesai.setText(String.valueOf(jadwalModel.getValueAt(r, 4)));
        jRuangan.setText(String.valueOf(jadwalModel.getValueAt(r, 5)));
    }

    private void clearJadwalForm() {
        editingJadwalId = null;
        jJamMulai.setText("");
        jJamSelesai.setText("");
        jRuangan.setText("");
        jadwalTable.clearSelection();
    }

    private void onJadwalAdd() {
        String kode = selectedKodeKelasForJadwal();
        if (kode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih kode kelas (bukan 'semua').", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JadwalKelas j = buildJadwalFromForm(UUID.randomUUID().toString(), kode);
        if (j.getJamMulai().isEmpty() || j.getJamSelesai().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Jam mulai dan jam selesai wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            jadwalService.save(j);
            JOptionPane.showMessageDialog(this, "Jadwal ditambahkan.", "OK", JOptionPane.INFORMATION_MESSAGE);
            reloadJadwalTable();
            clearJadwalForm();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onJadwalUpdate() {
        if (editingJadwalId == null || editingJadwalId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih baris jadwal di tabel.", "Perbarui", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String kode = selectedKodeKelasForJadwal();
        if (kode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih kode kelas.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JadwalKelas j = buildJadwalFromForm(editingJadwalId, kode);
        if (j.getJamMulai().isEmpty() || j.getJamSelesai().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Jam mulai dan jam selesai wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            jadwalService.updateById(editingJadwalId, j);
            JOptionPane.showMessageDialog(this, "Jadwal diperbarui.", "OK", JOptionPane.INFORMATION_MESSAGE);
            reloadJadwalTable();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onJadwalDelete() {
        int r = jadwalTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih jadwal.", "Hapus", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String id = String.valueOf(jadwalModel.getValueAt(r, 0));
        if (id.isEmpty()) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Hapus jadwal ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            jadwalService.deleteById(id);
            reloadJadwalTable();
            clearJadwalForm();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String selectedKodeKelasForJadwal() {
        Object o = jFilterKelas.getSelectedItem();
        if (o == null) {
            return "";
        }
        String s = o.toString().trim();
        if (s.isEmpty() || "(semua)".equals(s)) {
            return "";
        }
        return s;
    }

    private JadwalKelas buildJadwalFromForm(String id, String kodeKelas) {
        return new JadwalKelas(
                id,
                kodeKelas,
                String.valueOf(jHari.getSelectedItem()),
                jJamMulai.getText().trim(),
                jJamSelesai.getText().trim(),
                jRuangan.getText().trim());
    }

    // --- Absensi ---
    private void reloadAbsensiTable() {
        absensiModel.setRowCount(0);
        try {
            for (LogAbsensi log : absensiService.findAll()) {
                absensiModel.addRow(new Object[]{
                    safe(log.getIdLog()),
                    log.getWaktuTap() != null ? log.getWaktuTap().toString() : "",
                    safe(log.getNim()),
                    safe(log.getKodeKelas()),
                    safe(log.getStatus()),
                    safe(log.getUidRfid()),
                    safe(log.getKeterangan())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat log: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAbsensiSave() {
        String nim = "";
        if (aNim.getEditor().getEditorComponent() instanceof JTextField) {
            nim = ((JTextField) aNim.getEditor().getEditorComponent()).getText().trim();
        }
        Object ko = aKelas.getSelectedItem();
        String kode = ko != null ? ko.toString().trim() : "";
        if (nim.isEmpty()) {
            JOptionPane.showMessageDialog(this, "NIM wajib diisi.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (kode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih kelas.", "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date waktu = (Date) aWaktu.getValue();
        String id = UUID.randomUUID().toString();
        LogAbsensi log = new LogAbsensi(
                id,
                nim,
                kode,
                aRfid.getText().trim(),
                waktu,
                String.valueOf(aStatus.getSelectedItem()),
                aKet.getText().trim());
        try {
            absensiService.save(log);
            JOptionPane.showMessageDialog(this, "Log absensi disimpan.", "OK", JOptionPane.INFORMATION_MESSAGE);
            reloadAbsensiTable();
            aRfid.setText("");
            aKet.setText("");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAbsensiDelete() {
        int r = absensiTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Pilih baris log.", "Hapus", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String id = String.valueOf(absensiModel.getValueAt(r, 0));
        if (id.isEmpty()) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Hapus log ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            absensiService.deleteByIdLog(id);
            reloadAbsensiTable();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
