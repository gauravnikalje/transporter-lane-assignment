package com.assignment.transporter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tunable settings, bound from {@code transporter.*} in application.properties. */
@ConfigurationProperties(prefix = "transporter")
public record TransporterProperties(@DefaultValue Solver solver, @DefaultValue Limits limits) {

    /**
     * @param workBudget         lane evaluations the exact search may spend before it returns its best answer so far
     * @param coverageNodeBudget search nodes allowed when working out how many transporters a failed request needs
     */
    public record Solver(@DefaultValue("50000000") long workBudget, @DefaultValue("2000000") long coverageNodeBudget) {
    }

    /** Size caps that protect a small server from huge payloads. */
    public record Limits(@DefaultValue("10000") int maxLanes, @DefaultValue("1000") int maxTransporters,
                         @DefaultValue("200000") int maxQuotes, @DefaultValue("2000000") long maxMatrixCells) {
    }
}
