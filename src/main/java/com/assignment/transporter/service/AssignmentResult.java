package com.assignment.transporter.service;

import com.assignment.transporter.optimizer.AssignmentSolution;

/**
 * @param solution the chosen assignment
 * @param optimal  true when the search finished, so no cheaper assignment exists
 */
public record AssignmentResult(AssignmentSolution solution, boolean optimal) {
}
