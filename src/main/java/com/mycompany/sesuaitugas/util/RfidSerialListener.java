package com.mycompany.sesuaitugas.util;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;

/**
 * Listener serial port untuk membaca UID kartu RFID dari Arduino.
 *
 * <h3>Asumsi protokol Arduino:</h3>
 * <p>Arduino mengirim UID sebagai string ASCII diakhiri newline ({@code \n}),
 * contoh: {@code "A3F2B1C4\n"}. Satu tap kartu = satu baris.</p>
 *
 * <h3>Cara pakai:</h3>
 * <pre>
 * RfidSerialListener listener = new RfidSerialListener("COM3", uid -> {
 *     // uid = "A3F2B1C4" — dipanggil di background thread
 *     SwingUtilities.invokeLater(() -> prosesUid(uid));
 * });
 * listener.start();
 * // ...
 * listener.stop();
 * </pre>
 *
 * <h3>Format sketch Arduino minimal:</h3>
 * <pre>
 * #include &lt;SPI.h&gt;
 * #include &lt;MFRC522.h&gt;
 * MFRC522 mfrc522(10, 9);
 * void setup() {
 *   Serial.begin(9600);
 *   SPI.begin();
 *   mfrc522.PCD_Init();
 * }
 * void loop() {
 *   if (!mfrc522.PICC_IsNewCardPresent()) return;
 *   if (!mfrc522.PICC_ReadCardSerial()) return;
 *   String uid = "";
 *   for (byte i = 0; i &lt; mfrc522.uid.size; i++) {
 *     uid += String(mfrc522.uid.uidByte[i], HEX);
 *   }
 *   uid.toUpperCase();
 *   Serial.println(uid);
 *   delay(1000);
 * }
 * </pre>
 */
public class RfidSerialListener {

    /** Callback dipanggil setiap kali UID lengkap diterima. */
    public interface UidCallback {
        void onUidReceived(String uid);
    }

    private final String      portName;
    private final int         baudRate;
    private final UidCallback callback;

    private SerialPort port;
    private final StringBuilder buffer = new StringBuilder();

    /**
     * Buat listener baru.
     *
     * @param portName nama port serial, misal {@code "COM3"} (Windows)
     *                 atau {@code "/dev/ttyUSB0"} (Linux)
     * @param callback dipanggil di thread background saat UID diterima
     */
    public RfidSerialListener(String portName, UidCallback callback) {
        this(portName, 9600, callback);
    }

    /**
     * Buat listener dengan baud rate custom.
     *
     * @param portName nama port serial
     * @param baudRate baud rate (harus sama dengan sketch Arduino, default 9600)
     * @param callback dipanggil saat UID diterima
     */
    public RfidSerialListener(String portName, int baudRate, UidCallback callback) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.callback = callback;
    }

    /**
     * Buka koneksi serial dan mulai mendengarkan data dari Arduino.
     *
     * @return {@code true} jika port berhasil dibuka, {@code false} jika gagal
     *         (termasuk jika native library tidak tersedia)
     */
    public boolean start() {
        try {
            port = SerialPort.getCommPort(portName);
        } catch (Throwable t) {
            System.err.println("[RFID] Gagal inisialisasi SerialPort: " + t.getMessage());
            return false;
        }
        port.setBaudRate(baudRate);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (!port.openPort()) {
            System.err.println("[RFID] Gagal membuka port: " + portName);
            return false;
        }

        port.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    return;
                }
                int available = port.bytesAvailable();
                if (available <= 0) return;

                byte[] bytes = new byte[available];
                port.readBytes(bytes, available);
                String chunk = new String(bytes, StandardCharsets.US_ASCII);

                // Akumulasi karakter sampai ada newline
                buffer.append(chunk);
                int idx;
                while ((idx = buffer.indexOf("\n")) >= 0) {
                    String uid = buffer.substring(0, idx).trim();
                    buffer.delete(0, idx + 1);
                    if (!uid.isEmpty()) {
                        callback.onUidReceived(uid.toUpperCase());
                    }
                }
            }
        });

        System.out.println("[RFID] Listener aktif di " + portName + " (" + baudRate + " baud)");
        return true;
    }

    /**
     * Tutup koneksi serial dan hentikan listener.
     */
    public void stop() {
        if (port != null && port.isOpen()) {
            port.removeDataListener();
            port.closePort();
            System.out.println("[RFID] Koneksi serial " + portName + " ditutup.");
        }
    }

    /** @return {@code true} jika port sedang terbuka */
    public boolean isRunning() {
        return port != null && port.isOpen();
    }

    /**
     * Utility: mendapatkan daftar semua port serial yang tersedia di sistem.
     * Berguna untuk menampilkan pilihan COM port di UI.
     * Mengembalikan array kosong jika native library tidak tersedia.
     *
     * @return array nama port (misal {@code ["COM3", "COM5"]}), atau array kosong jika gagal
     */
    public static String[] getAvailablePorts() {
        try {
            SerialPort[] ports = SerialPort.getCommPorts();
            String[] names = new String[ports.length];
            for (int i = 0; i < ports.length; i++) {
                names[i] = ports[i].getSystemPortName();
            }
            return names;
        } catch (Throwable t) {
            // Native library tidak tersedia (arsitektur tidak cocok, dll)
            System.err.println("[RFID] jSerialComm tidak tersedia: " + t.getMessage());
            return new String[0];
        }
    }
}
