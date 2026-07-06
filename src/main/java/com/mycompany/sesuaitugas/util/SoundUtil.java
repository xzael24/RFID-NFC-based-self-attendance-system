package com.mycompany.sesuaitugas.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundUtil {

    public static void playSuccess() {
        new Thread(() -> {
            boolean played = false;
            try (InputStream res = SoundUtil.class.getResourceAsStream("/sound/success.wav")) {
                if (res != null) {
                    AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(res));
                    Clip clip = AudioSystem.getClip();
                    clip.open(stream);
                    clip.start();
                    Thread.sleep(clip.getMicrosecondLength() / 1000 + 50);
                    clip.close();
                    stream.close();
                    played = true;
                }
            } catch (Exception e) { /* fallthrough */ }

            if (!played) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }).start();
    }
}
