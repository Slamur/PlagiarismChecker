package com.slamur.plagiarism.logic;

import java.util.Arrays;

public class DisjointSetUnion {

    int[] ranks;
    int[] parents;

    DisjointSetUnion(int size) {
        this.ranks = new int[size];
        Arrays.fill(ranks, 1);

        this.parents = new int[size];
        for (int i = 0; i < size; ++i) {
            parents[i] = i;
        }
    }

    int get(int v) {
        if (v == parents[v]) return v;
        return parents[v] = get(parents[v]);
    }

    @SuppressWarnings("unused")
    boolean union(int a, int b, int index) {
        a = get(a);
        b = get(b);

        boolean united = false;
        if (a != b) {
            if (ranks[a] < ranks[b]) {
                int tmp = a;
                a = b;
                b = tmp;
            }

            parents[b] = a;
            if (ranks[a] == ranks[b]) ++ranks[a];

            united = true;
        }

        return united;
    }

    @SuppressWarnings("unused")
    boolean connected(int a, int b) {
        return get(a) == get(b);
    }
}
