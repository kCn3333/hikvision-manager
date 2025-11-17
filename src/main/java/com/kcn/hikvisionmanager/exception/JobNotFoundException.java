package com.kcn.hikvisionmanager.exception;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String jobId) {
        super("Download job not found: " + jobId);
    }
}
