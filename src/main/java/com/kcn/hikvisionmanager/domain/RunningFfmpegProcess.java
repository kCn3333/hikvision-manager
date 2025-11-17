package com.kcn.hikvisionmanager.domain;

import java.util.concurrent.Future;


public record RunningFfmpegProcess(
        Process process,
        Future<?> stderrReader
) {}