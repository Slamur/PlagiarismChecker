package com.slamur.plagiarism.logic;

import java.util.Arrays;

public class MatchingSimilarityCalculator {

    private final double[][] sims;

    private final int[][] graph;
    private final int[] mt;

    private final int[] colors;

    public MatchingSimilarityCalculator(double[][] sims) {
        this.sims = sims;

        int leftSize = sims.length, rightSize = sims[0].length;

        this.graph = new int[leftSize][rightSize];
        this.mt = new int[rightSize];

        this.colors = new int[leftSize];
    }

    public double calculate(double filter) {
        double left = 0, right = 1;
        for (int it = 0; it < 100; ++it) {
            double mid = (left + right) / 2;

            double ratio = ratio(mid);
            if (ratio >= filter) {
                left = mid;
            } else {
                right = mid;
            }
        }

        return left;
    }

    private double ratio(double minSim) {
        for (int v = 0; v < graph.length; ++v) {
            for (int u = 0, i = 0; u < sims[v].length; ++u) {
                if (sims[v][u] >= minSim) {
                    graph[v][i++] = u;
                }
            }
        }

        Arrays.fill(mt, -1);
        Arrays.fill(colors, -1);

        for (int i = 0; i < graph.length; ++i) {
            dfs(i, i);
        }

        int total = (colors.length + mt.length);
        total = Math.max(total, 1);

        int matched = 0;
        for (int value : mt) {
            if (value != -1) {
                matched++;
            }
        }

        return 2.0 * matched / total;
    }

    private boolean dfs(int from, int color) {
        if (colors[from] == color) {
            return false;
        }
        colors[from] = color;

        for (int to : graph[from]) {
            if (mt[to] == -1) {
                mt[to] = from;
                return true;
            }
        }

        for (int to : graph[from]) {
            if (dfs(mt[to], color)) {
                mt[to] = from;
                return true;
            }
        }

        return false;
    }
}