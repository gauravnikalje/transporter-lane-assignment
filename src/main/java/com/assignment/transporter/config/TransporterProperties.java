package com.assignment.transporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tunable settings, bound from {@code transporter.*} in application.properties. */
@ConfigurationProperties(prefix = "transporter")
public record TransporterProperties(@DefaultValue Solver solver, @DefaultValue Limits limits) {

    /**
     * All three are counted in lane evaluations, so the time they allow does not depend on the input's shape.
     *
     * @param workBudget         the exact search may spend this much before it returns its best answer so far
     * @param seedWorkBudget     the greedy and swap search that seeds the exact one
     * @param coverageWorkBudget working out how many transporters a failed request would need
     */
    public record Solver(@DefaultValue("25000000") long workBudget, @DefaultValue("10000000") long seedWorkBudget,
                         @DefaultValue("20000000") long coverageWorkBudget) {
    }

    /** Size caps that protect a small server from huge payloads. */
    public record Limits(@DefaultValue("2000") int maxLanes, @DefaultValue("200") int maxTransporters,
                         @DefaultValue("10000") int maxQuotes, @DefaultValue("200000") long maxMatrixCells,
                         @DefaultValue("1048576") long maxBodyBytes) {
    }
}
