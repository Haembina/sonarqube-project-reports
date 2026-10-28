package com.haembina.projectreports.report;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;

public record Report(ReportTarget target, Set<ReportSection> sections, QualityGate qualityGate,
    List<Measure> measures, List<Issue> issues, List<Hotspot> hotspots, List<FileMeasures> files,
    List<LineCoverage> coverage, List<Duplication> duplications, boolean sourceHidden, Links links) {

    public Report {
        Set<ReportSection> ordered = EnumSet.noneOf(ReportSection.class);
        ordered.addAll(sections);
        sections = Collections.unmodifiableSet(ordered);
        measures = List.copyOf(measures);
        issues = List.copyOf(issues);
        hotspots = List.copyOf(hotspots);
        files = List.copyOf(files);
        coverage = List.copyOf(coverage);
        duplications = List.copyOf(duplications);
    }

    public Report(ReportTarget target, QualityGate qualityGate, List<Measure> measures, List<Issue> issues,
        List<Hotspot> hotspots, List<FileMeasures> files) {
        this(target, ReportSection.all(), qualityGate, measures, issues, hotspots, files, List.of(), List.of(),
            false, Links.NONE);
    }

    public Report(ReportTarget target, Set<ReportSection> sections, QualityGate qualityGate, List<Measure> measures,
        List<Issue> issues, List<Hotspot> hotspots, List<FileMeasures> files, List<LineCoverage> coverage,
        List<Duplication> duplications, boolean sourceHidden) {
        this(target, sections, qualityGate, measures, issues, hotspots, files, coverage, duplications, sourceHidden,
            Links.NONE);
    }

    public boolean has(ReportSection section) {
        return sections.contains(section);
    }

    public record Links(@Nullable String repository, @Nullable String repositoryUrl, @Nullable String scopeUrl,
        @Nullable String sonarUrl, @Nullable String pullRequestTitle) {

        public static final Links NONE = new Links(null, null, null, null, null);
    }

    public record LineCoverage(String path, List<Integer> uncovered, List<PartLine> partial) {

        public LineCoverage {
            uncovered = List.copyOf(uncovered);
            partial = List.copyOf(partial);
        }
    }

    public record PartLine(int line, int covered, int conditions) {
    }

    public record Duplication(String path, int from, int to, List<Place> copies) {

        public Duplication {
            copies = List.copyOf(copies);
        }
    }

    public record Place(String path, int from, int to) {
    }

    public record QualityGate(@Nullable String status, List<Condition> conditions) {

        public QualityGate {
            conditions = List.copyOf(conditions);
        }
    }

    public record Condition(String metric, String comparator, String threshold, String actual, String status) {
    }

    public record Measure(String metric, @Nullable String value) {
    }

    public record Issue(String key, String rule, @Nullable String severity, @Nullable String type,
        @Nullable String file, @Nullable Integer line, String message, @Nullable String status,
        @Nullable String creationDate) {
    }

    public record Hotspot(String key, String rule, @Nullable String file, @Nullable Integer line, String message,
        @Nullable String securityCategory, @Nullable String vulnerabilityProbability, @Nullable String status,
        @Nullable String resolution) {
    }

    public record FileMeasures(String key, String path, Map<String, String> measures) {

        public FileMeasures {
            measures = new TreeMap<>(measures);
        }

        @Nullable
        public String measure(String metric) {
            return measures.get(metric);
        }
    }
}
