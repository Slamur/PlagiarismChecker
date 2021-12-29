package com.slamur.plagiarism.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.slamur.plagiarism.model.parsing.solution.SolutionProgramLine;

public class MatchingSimilarityCalculator {

    static class Edge {

        private final int to;
        private final double sim;

        public Edge(int to, double sim) {
            this.to = to;
            this.sim = sim;
        }
    }

    public static double calculate(List<SolutionProgramLine> leftLines,
                                   List<SolutionProgramLine> rightLines,
                                   double filter,
                                   double minimalSimilarityLimit) {
        int leftSize = leftLines.size();
        int rightSize = rightLines.size();

        if (leftSize > rightSize) {
            var tmpRows = leftLines;
            leftLines = rightLines;
            rightLines = tmpRows;

            leftSize = leftLines.size();
            rightSize = rightLines.size();
        }

        List<Edge>[] edges = new List[leftSize];
        for (int v = 0; v < leftSize; ++v) edges[v] = new ArrayList<>(0);

        for (int i = 0; i < leftSize; ++i) {
            var leftLine = leftLines.get(i);
            for (int j = 0; j < rightSize; ++j) {
                double sim = leftLine.calculateSimilarity(rightLines.get(j));

                if (sim >= minimalSimilarityLimit) {
                    edges[i].add(new Edge(j, sim));
                }
            }
        }

        return new MatchingSimilarityCalculator(edges, rightSize, minimalSimilarityLimit).calculate(filter);
    }

    private final List<Edge>[] edges;
    private final double minimalSimilarityLimit;

    private final List<Integer>[] graph;
    private final int[] mt;

    private final int[] colors;

    public MatchingSimilarityCalculator(List<Edge>[] edges,
                                        int rightSize,
                                        double minimalSimilarityLimit) {
        this.edges = edges;
        this.minimalSimilarityLimit = minimalSimilarityLimit;

        int leftSize = edges.length;

        this.graph = new List[leftSize];
        for (int v = 0; v < leftSize; ++v) graph[v] = new ArrayList<>(0);
        this.mt = new int[rightSize];

        this.colors = new int[leftSize];
    }

    public double calculate(double filter) {
        double left = minimalSimilarityLimit, right = 1;
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
            graph[v].clear();
            for (var edge : edges[v]){
                if (edge.sim >= minSim) {
                    graph[v].add(edge.to);
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