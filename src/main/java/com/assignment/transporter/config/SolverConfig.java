package com.assignment.transporter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.assignment.transporter.optimizer.AssignmentSolver;
import com.assignment.transporter.optimizer.BranchAndBoundSolver;
import com.assignment.transporter.optimizer.CoverageAnalyzer;
import com.assignment.transporter.optimizer.GreedyLocalSearchSolver;

/** Wires the solver strategy. Swap the bean to change the algorithm; nothing else depends on the choice. */
@Configuration
@EnableConfigurationProperties(TransporterProperties.class)
public class SolverConfig {

    @Bean
    AssignmentSolver assignmentSolver(TransporterProperties properties) {
        return new BranchAndBoundSolver(new GreedyLocalSearchSolver(properties.solver().seedWorkBudget()),
                properties.solver().workBudget());
    }

    @Bean
    CoverageAnalyzer coverageAnalyzer(TransporterProperties properties) {
        return new CoverageAnalyzer(properties.solver().coverageWorkBudget());
    }
}
