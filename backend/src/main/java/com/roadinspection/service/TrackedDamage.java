package com.roadinspection.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class TrackedDamage {
    private String className;
    private List<double[]> bboxes = new ArrayList<>();
    private List<Double> confidences = new ArrayList<>();
    private List<Integer> frameIndices = new ArrayList<>();
    private List<Long> timestamps = new ArrayList<>();
    
    private double[] predictedBbox;
    private int lastSeenFrame;
    private long firstSeenTime;
    private long lastSeenTime;
    
    private double[] kalmanState;
    private double[][] kalmanCovariance;
    
    private static final double PROCESS_NOISE = 1.0;
    private static final double MEASUREMENT_NOISE = 1.0;
    
    public TrackedDamage(String className, double[] bbox, int frameIndex, long timestamp) {
        this.className = className;
        this.bboxes.add(bbox.clone());
        this.frameIndices.add(frameIndex);
        this.timestamps.add(timestamp);
        this.lastSeenFrame = frameIndex;
        this.firstSeenTime = timestamp;
        this.lastSeenTime = timestamp;
        this.predictedBbox = bbox.clone();
        
        initializeKalmanFilter(bbox);
    }
    
    private void initializeKalmanFilter(double[] initialBbox) {
        kalmanState = new double[]{
            (initialBbox[0] + initialBbox[2]) / 2,
            (initialBbox[1] + initialBbox[3]) / 2,
            initialBbox[2] - initialBbox[0],
            initialBbox[3] - initialBbox[1],
            0, 0
        };
        
        kalmanCovariance = new double[6][6];
        for (int i = 0; i < 6; i++) {
            kalmanCovariance[i][i] = 1.0;
        }
    }
    
    public void addDetection(Map<String, Object> detection, int frameIndex, long timestamp) {
        double[] bbox = extractBbox(detection);
        double confidence = ((Number) detection.get("confidence")).doubleValue();
        
        bboxes.add(bbox);
        confidences.add(confidence);
        frameIndices.add(frameIndex);
        timestamps.add(timestamp);
        
        lastSeenFrame = frameIndex;
        lastSeenTime = timestamp;
        
        updateKalmanFilter(bbox);
    }
    
    private void updateKalmanFilter(double[] measurement) {
        predict();
        
        double[] measurementVector = new double[]{
            (measurement[0] + measurement[2]) / 2,
            (measurement[1] + measurement[3]) / 2,
            measurement[2] - measurement[0],
            measurement[3] - measurement[1]
        };
        
        update(measurementVector);
        
        double centerX = kalmanState[0];
        double centerY = kalmanState[1];
        double width = kalmanState[2];
        double height = kalmanState[3];
        
        predictedBbox = new double[]{
            centerX - width / 2,
            centerY - height / 2,
            centerX + width / 2,
            centerY + height / 2
        };
    }
    
    private void predict() {
        kalmanState[0] += kalmanState[4];
        kalmanState[1] += kalmanState[5];
        
        for (int i = 0; i < 6; i++) {
            kalmanCovariance[i][i] += PROCESS_NOISE;
        }
    }
    
    private void update(double[] measurement) {
        double[][] H = new double[4][6];
        H[0][0] = 1; H[1][1] = 1;
        H[2][2] = 1; H[3][3] = 1;
        
        double[][] Ht = new double[6][4];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                Ht[i][j] = H[j][i];
            }
        }
        
        double[][] S = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 6; k++) {
                    S[i][j] += H[i][k] * kalmanCovariance[k][j] * Ht[k][j];
                }
                S[i][j] += MEASUREMENT_NOISE;
            }
        }
        
        double[] y = new double[4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                y[i] += H[i][j] * kalmanState[j];
            }
            y[i] = measurement[i] - y[i];
        }
        
        double[][] K = new double[6][4];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    K[i][j] += kalmanCovariance[i][k] * Ht[k][j];
                }
                double denom = 0;
                for (int k = 0; k < 4; k++) {
                    denom += S[j][k];
                }
                if (denom != 0) {
                    K[i][j] /= denom;
                }
            }
        }
        
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                kalmanState[i] += K[i][j] * y[j];
            }
        }
        
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 4; k++) {
                    kalmanCovariance[i][j] -= K[i][k] * H[k][j];
                }
            }
        }
    }
    
    public void updatePrediction(double[] bbox, int frameIndex, long timestamp) {
        bboxes.add(bbox.clone());
        frameIndices.add(frameIndex);
        timestamps.add(timestamp);
        lastSeenFrame = frameIndex;
        lastSeenTime = timestamp;
        
        updateKalmanFilter(bbox);
    }
    
    public DamageRecord toDamageRecord() {
        DamageRecord record = new DamageRecord();
        record.setClassName(className);
        
        double[] avgBbox = calculateAverageBbox();
        record.setBbox(avgBbox);
        
        double avgConfidence = confidences.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        record.setAverageConfidence(avgConfidence);
        
        record.setFirstFrame(frameIndices.get(0));
        record.setLastFrame(frameIndices.get(frameIndices.size() - 1));
        record.setFirstTimestamp(firstSeenTime);
        record.setLastTimestamp(lastSeenTime);
        record.setDuration(lastSeenTime - firstSeenTime);
        record.setFrameCount(frameIndices.size());
        record.setDetectionCount(bboxes.size());
        
        return record;
    }
    
    private double[] calculateAverageBbox() {
        double[] avg = new double[4];
        for (double[] bbox : bboxes) {
            for (int i = 0; i < 4; i++) {
                avg[i] += bbox[i];
            }
        }
        for (int i = 0; i < 4; i++) {
            avg[i] /= bboxes.size();
        }
        return avg;
    }
    
    private double[] extractBbox(Map<String, Object> det) {
        @SuppressWarnings("unchecked")
        Map<String, Object> bboxMap = (Map<String, Object>) det.get("bbox");
        return new double[]{
            ((Number) bboxMap.get("x1")).doubleValue(),
            ((Number) bboxMap.get("y1")).doubleValue(),
            ((Number) bboxMap.get("x2")).doubleValue(),
            ((Number) bboxMap.get("y2")).doubleValue()
        };
    }
    
    public int getLastSeenFrame() {
        return lastSeenFrame;
    }
    
    public double[] getPredictedBbox() {
        return predictedBbox;
    }
    
    public String getClassName() {
        return className;
    }
}
