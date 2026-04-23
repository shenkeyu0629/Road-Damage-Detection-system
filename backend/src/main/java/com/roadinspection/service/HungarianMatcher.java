package com.roadinspection.service;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class HungarianMatcher {
    
    public static Map<Integer, Integer> match(List<double[]> predictions, List<double[]> detections, double iouThreshold) {
        int n = predictions.size();
        int m = detections.size();
        
        if (n == 0 || m == 0) {
            return new HashMap<>();
        }
        
        double[][] costMatrix = new double[n][m];
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                double iou = calculateIOU(predictions.get(i), detections.get(j));
                costMatrix[i][j] = 1.0 - iou;
            }
        }
        
        return hungarianAlgorithm(costMatrix);
    }
    
    private static double calculateIOU(double[] bbox1, double[] bbox2) {
        double x1 = Math.max(bbox1[0], bbox2[0]);
        double y1 = Math.max(bbox1[1], bbox2[1]);
        double x2 = Math.min(bbox1[2], bbox2[2]);
        double y2 = Math.min(bbox1[3], bbox2[3]);
        
        double intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        
        double area1 = (bbox1[2] - bbox1[0]) * (bbox1[3] - bbox1[1]);
        double area2 = (bbox2[2] - bbox2[0]) * (bbox2[3] - bbox2[1]);
        
        double union = area1 + area2 - intersection;
        
        return union > 0 ? intersection / union : 0;
    }
    
    private static Map<Integer, Integer> hungarianAlgorithm(double[][] cost) {
        int n = cost.length;
        int m = cost[0].length;
        
        double[] u = new double[n + 1];
        double[] v = new double[m + 1];
        int[] p = new int[m + 1];
        int[] way = new int[m + 1];
        
        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[m + 1];
            Arrays.fill(minv, Double.MAX_VALUE);
            boolean[] used = new boolean[m + 1];
            
            do {
                used[j0] = true;
                int i0 = p[j0];
                double delta = Double.MAX_VALUE;
                int j1 = 0;
                
                for (int j = 1; j <= m; j++) {
                    if (!used[j]) {
                        double cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }
                
                for (int j = 0; j <= m; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                
                j0 = j1;
            } while (p[j0] != 0);
            
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }
        
        Map<Integer, Integer> result = new HashMap<>();
        for (int j = 1; j <= m; j++) {
            if (p[j] != 0) {
                result.put(p[j] - 1, j - 1);
            }
        }
        
        return result;
    }
}
