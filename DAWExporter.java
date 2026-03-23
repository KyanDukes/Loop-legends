package com.beatmaker.game.daw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DAWExporter {
    
    private static final int SAMPLE_RATE = 44100;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Stereo
    
    public static void exportToWAV(
        String filename,
        DAWGrid grid,
        DAWMixer mixer,
        DAWTransport transport,
        int loopCount
    ) {
        System.out.println("Starting WAV export to: " + filename);
        
        try {
            float bpm = transport.getBpm();
            int steps = DAWGrid.STEPS; // FIXED: Use constant instead of method
            float stepDuration = 60f / bpm / 4f;
            float totalDuration = stepDuration * steps * loopCount;
            
            int totalSamples = (int)(totalDuration * SAMPLE_RATE);
            int bufferSize = totalSamples * CHANNELS * (BITS_PER_SAMPLE / 8);
            
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            // Generate silence (placeholder for actual audio mixing)
            for (int i = 0; i < totalSamples * CHANNELS; i++) {
                buffer.putShort((short) 0);
            }
            
            FileHandle file = Gdx.files.local(filename);
            writeWAVHeader(file, buffer.array(), SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE);
            
            System.out.println("WAV export completed: " + filename);
            System.out.println("Duration: " + totalDuration + " seconds");
            
        } catch (Exception e) {
            System.err.println("Error exporting WAV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void writeWAVHeader(
        FileHandle file,
        byte[] audioData,
        int sampleRate,
        int channels,
        int bitsPerSample
    ) throws IOException {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        
        int dataSize = audioData.length;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        
        // RIFF header
        dos.writeBytes("RIFF");
        dos.writeInt(Integer.reverseBytes(36 + dataSize));
        dos.writeBytes("WAVE");
        
        // fmt chunk
        dos.writeBytes("fmt ");
        dos.writeInt(Integer.reverseBytes(16));
        dos.writeShort(Short.reverseBytes((short) 1));
        dos.writeShort(Short.reverseBytes((short) channels));
        dos.writeInt(Integer.reverseBytes(sampleRate));
        dos.writeInt(Integer.reverseBytes(byteRate));
        dos.writeShort(Short.reverseBytes((short) blockAlign));
        dos.writeShort(Short.reverseBytes((short) bitsPerSample));
        
        // data chunk
        dos.writeBytes("data");
        dos.writeInt(Integer.reverseBytes(dataSize));
        dos.write(audioData);
        
        file.writeBytes(out.toByteArray(), false);
        dos.close();
    }
    
    public static void exportToMIDI(
        String filename,
        DAWGrid grid,
        DAWTransport transport
    ) {
        System.out.println("MIDI export not yet implemented");
    }
}