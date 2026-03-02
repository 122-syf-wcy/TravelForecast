package com.travel.dto.system;

import lombok.Data;

import java.util.Date;

/**
 * 数据库备份信息DTO
 */
@Data
public class BackupInfo {
    private String id;
    private String filename;
    private Date date;
    private String size;
    private Long sizeBytes;
    private String type;
    private String path;
}

