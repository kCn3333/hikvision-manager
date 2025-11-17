package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CameraStorageDTO {

    private int id;
    private String name;
    private String path;
    private String type;
    private String status;
    private String capacity;
    private String usage;
    private String property;
    private String formatType;
    private List<String> mountTypes;
    private List<String> authentications;

}
