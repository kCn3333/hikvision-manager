package com.kcn.hikvisionmanager.service.ffmpeg;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class FFmpegCommandBuilder {

    /**
     * Build FFmpeg command for RTSP download
     */
    public List<String> buildFFmpegDownloadCommand(String rtspUrl, Path outputPath) {
        List<String> command = new ArrayList<>();

//        command.add("ffmpeg");
//        command.add("-rtsp_transport");
//        command.add("tcp");
//        command.add("-fflags");
//        command.add("nobuffer");    // ✅ Krytyczne - zmniejsza buforowanie
//        command.add("-i");
//        command.add(rtspUrl);       // ✅ Użyj playbackUrl z odpowiedzi kamery
//        command.add("-c");
//        command.add("copy");        // ✅ Bez re-encoding - najszybsze
//        command.add("-f");
//        command.add("mp4");
//        command.add("-movflags");
//        command.add("faststart");   // ✅ Umożliwia streaming
//        command.add("-y");
//        command.add(outputPath.toString());
//
//        return command;
        command.add("ffmpeg");
        command.add("-rtsp_transport");
        command.add("tcp");              // Use TCP transport for RTSP for better reliability
        command.add("-i");               // Input
        command.add(rtspUrl);            // [INPUT] Specify input source (RTSP URL in this case)
        command.add("-c");               // Codec
        command.add("copy");             // [CODEC] Use stream copy - no re-encoding, fastest method
        command.add("-y");               // Overwrite output file without asking
        command.add(outputPath.toString()); // Output file path

        return command;
    }

    /**
     * Build FFmpeg command for RTSP stream
     */
    public List<String> buildHlsCommand(String rtsp, Path outDir) {
        List<String> cmd = new ArrayList<>();

        cmd.add("ffmpeg");
        cmd.add("-rtsp_transport");
        cmd.add("tcp");
        cmd.add("-i");
        cmd.add(rtsp);
        cmd.add("-c");
        cmd.add("copy");              // REMUX – minimalny narzut CPU
        cmd.add("-f");
        cmd.add("hls");
        cmd.add("-hls_time");
        cmd.add("2");                 // 2s segmenty
        cmd.add("-hls_list_size");
        cmd.add("5");
        cmd.add("-hls_flags");
        cmd.add("delete_segments+append_list+omit_endlist");
        cmd.add("-hls_segment_filename");
        cmd.add(outDir.resolve("segment_%03d.ts").toString());
        cmd.add(outDir.resolve("index.m3u8").toString());

        return cmd;
    }

}
