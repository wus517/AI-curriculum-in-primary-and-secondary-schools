package com.example.oj.learning.tracking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.example.oj.platform.PlatformDtos.Interval;

public final class PlaybackIntervals {
    private PlaybackIntervals() {}

    public static List<Interval> merge(List<Interval> existing, List<Interval> incoming) {
        var sorted = new ArrayList<>(existing); sorted.addAll(incoming);
        sorted.sort(Comparator.comparingDouble(Interval::start).thenComparingDouble(Interval::end));
        var merged = new ArrayList<Interval>();
        for (Interval interval : sorted) {
            if (merged.isEmpty() || interval.start() > merged.get(merged.size() - 1).end()) {
                merged.add(interval);
            } else {
                var last = merged.remove(merged.size() - 1);
                merged.add(new Interval(last.start(), Math.max(last.end(), interval.end())));
            }
        }
        return List.copyOf(merged);
    }

    public static double seconds(List<Interval> intervals) {
        return intervals.stream().mapToDouble(i -> i.end() - i.start()).sum();
    }
}
