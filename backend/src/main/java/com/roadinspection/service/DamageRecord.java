package com.roadinspection.service;

import lombok.Data;

@Data
public class DamageRecord {
    private String className;
    private double[] bbox;
    private double averageConfidence;
    private int firstFrame;
    private int lastFrame;
    private long firstTimestamp;
    private long lastTimestamp;
    private long duration;
    private int frameCount;
    private int detectionCount;
    private String level;
    private double area;
}
