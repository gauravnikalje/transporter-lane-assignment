package com.assignment.transporter.api;

/**
 * Example request bodies shown in Swagger UI, so a reviewer can run the brief's cases in one click.
 * Generated from the brief and the provided data file; the tests post the same payloads.
 */
public final class ApiExamples {

    private ApiExamples() {
    }

    /** Brief Test Case 1: 5 lanes, 7 transporters, every transporter quotes every lane. */
    public static final String TEST_CASE_1 = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 20835}, {"laneId": 2, "quote": 10512}, {"laneId": 3, "quote": 22105}, {"laneId": 4, "quote": 42481}, {"laneId": 5, "quote": 19862}]},
                {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 1, "quote": 48844}, {"laneId": 2, "quote": 31326}, {"laneId": 3, "quote": 18640}, {"laneId": 4, "quote": 45828}, {"laneId": 5, "quote": 18297}]},
                {"id": 3, "name": "Transporter T3", "laneQuotes": [{"laneId": 1, "quote": 39020}, {"laneId": 2, "quote": 20648}, {"laneId": 3, "quote": 31438}, {"laneId": 4, "quote": 36447}, {"laneId": 5, "quote": 12789}]},
                {"id": 4, "name": "Transporter T4", "laneQuotes": [{"laneId": 1, "quote": 14400}, {"laneId": 2, "quote": 44514}, {"laneId": 3, "quote": 14316}, {"laneId": 4, "quote": 10678}, {"laneId": 5, "quote": 13032}]},
                {"id": 5, "name": "Transporter T5", "laneQuotes": [{"laneId": 1, "quote": 11601}, {"laneId": 2, "quote": 19760}, {"laneId": 3, "quote": 40870}, {"laneId": 4, "quote": 20635}, {"laneId": 5, "quote": 26421}]},
                {"id": 6, "name": "Transporter T6", "laneQuotes": [{"laneId": 1, "quote": 35095}, {"laneId": 2, "quote": 12494}, {"laneId": 3, "quote": 17808}, {"laneId": 4, "quote": 36210}, {"laneId": 5, "quote": 39444}]},
                {"id": 7, "name": "Transporter T7", "laneQuotes": [{"laneId": 1, "quote": 26070}, {"laneId": 2, "quote": 41098}, {"laneId": 3, "quote": 20932}, {"laneId": 4, "quote": 16897}, {"laneId": 5, "quote": 27938}]}
              ]
            }
            """;

    /** Brief Test Case 2: sparse quotes. Needs 3 transporters, so a limit of 2 is infeasible. */
    public static final String TEST_CASE_2 = """
            {
              "lanes": [
                {"id": 1, "origin": "Chandigarh", "destination": "Shimla"},
                {"id": 2, "origin": "Agra", "destination": "Kanpur"},
                {"id": 3, "origin": "Varanasi", "destination": "Gorakhpur"},
                {"id": 4, "origin": "Amritsar", "destination": "Ludhiana"},
                {"id": 5, "origin": "Coimbatore", "destination": "Madurai"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter X", "laneQuotes": [{"laneId": 1, "quote": 1000}, {"laneId": 2, "quote": 1500}]},
                {"id": 2, "name": "Transporter Y", "laneQuotes": [{"laneId": 3, "quote": 2000}, {"laneId": 4, "quote": 1800}]},
                {"id": 3, "name": "Transporter Z", "laneQuotes": [{"laneId": 5, "quote": 1200}]},
                {"id": 4, "name": "Transporter W", "laneQuotes": [{"laneId": 1, "quote": 1100}, {"laneId": 4, "quote": 1750}]}
              ]
            }
            """;

    /** The provided transporter_assignment_data.csv: 9 lanes, 7 transporters. */
    public static final String DATA_FILE_9_LANES = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"},
                {"id": 6, "origin": "Guwahati", "destination": "Shillong"},
                {"id": 7, "origin": "Lucknow", "destination": "Patna"},
                {"id": 8, "origin": "Ranchi", "destination": "Bhubaneswar"},
                {"id": 9, "origin": "Surat", "destination": "Indore"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 20835}, {"laneId": 2, "quote": 10512}, {"laneId": 3, "quote": 22105}, {"laneId": 4, "quote": 42481}, {"laneId": 5, "quote": 19862}, {"laneId": 6, "quote": 13567}, {"laneId": 7, "quote": 10015}, {"laneId": 8, "quote": 17886}, {"laneId": 9, "quote": 43996}]},
                {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 1, "quote": 48844}, {"laneId": 2, "quote": 31326}, {"laneId": 3, "quote": 18640}, {"laneId": 4, "quote": 45828}, {"laneId": 5, "quote": 18297}, {"laneId": 6, "quote": 45810}, {"laneId": 7, "quote": 49573}, {"laneId": 8, "quote": 45122}, {"laneId": 9, "quote": 31856}]},
                {"id": 3, "name": "Transporter T3", "laneQuotes": [{"laneId": 1, "quote": 39020}, {"laneId": 2, "quote": 20648}, {"laneId": 3, "quote": 31438}, {"laneId": 4, "quote": 36447}, {"laneId": 5, "quote": 12789}, {"laneId": 6, "quote": 49985}, {"laneId": 7, "quote": 35285}, {"laneId": 8, "quote": 10281}, {"laneId": 9, "quote": 40092}]},
                {"id": 4, "name": "Transporter T4", "laneQuotes": [{"laneId": 1, "quote": 14400}, {"laneId": 2, "quote": 44514}, {"laneId": 3, "quote": 14316}, {"laneId": 4, "quote": 10678}, {"laneId": 5, "quote": 13032}, {"laneId": 6, "quote": 46024}, {"laneId": 7, "quote": 42342}, {"laneId": 8, "quote": 35742}, {"laneId": 9, "quote": 48921}]},
                {"id": 5, "name": "Transporter T5", "laneQuotes": [{"laneId": 1, "quote": 11601}, {"laneId": 2, "quote": 19760}, {"laneId": 3, "quote": 40870}, {"laneId": 4, "quote": 20635}, {"laneId": 5, "quote": 26421}, {"laneId": 6, "quote": 28809}, {"laneId": 7, "quote": 27815}, {"laneId": 8, "quote": 17024}, {"laneId": 9, "quote": 44691}]},
                {"id": 6, "name": "Transporter T6", "laneQuotes": [{"laneId": 1, "quote": 35095}, {"laneId": 2, "quote": 12494}, {"laneId": 3, "quote": 17808}, {"laneId": 4, "quote": 36210}, {"laneId": 5, "quote": 39444}, {"laneId": 6, "quote": 29948}, {"laneId": 7, "quote": 17320}, {"laneId": 8, "quote": 10492}, {"laneId": 9, "quote": 37864}]},
                {"id": 7, "name": "Transporter T7", "laneQuotes": [{"laneId": 1, "quote": 26070}, {"laneId": 2, "quote": 41098}, {"laneId": 3, "quote": 20932}, {"laneId": 4, "quote": 16897}, {"laneId": 5, "quote": 27938}, {"laneId": 6, "quote": 43517}, {"laneId": 7, "quote": 25881}, {"laneId": 8, "quote": 46136}, {"laneId": 9, "quote": 31286}]}
              ]
            }
            """;

    /** The brief's first example: 10 lanes. Each lane has one or two bidders, so 5 transporters are needed. */
    public static final String BRIEF_MAIN_EXAMPLE = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"},
                {"id": 6, "origin": "Guwahati", "destination": "Shillong"},
                {"id": 7, "origin": "Lucknow", "destination": "Patna"},
                {"id": 8, "origin": "Ranchi", "destination": "Bhubaneswar"},
                {"id": 9, "origin": "Surat", "destination": "Indore"},
                {"id": 10, "origin": "Nagpur", "destination": "Raipur"}
              ],
              "transporters": [
                {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 5000}, {"laneId": 2, "quote": 6000}]},
                {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 3, "quote": 7000}, {"laneId": 4, "quote": 4000}]},
                {"id": 3, "name": "Transporter T3", "laneQuotes": [{"laneId": 5, "quote": 3000}, {"laneId": 6, "quote": 2000}]},
                {"id": 4, "name": "Transporter T4", "laneQuotes": [{"laneId": 7, "quote": 8000}, {"laneId": 8, "quote": 5000}]},
                {"id": 5, "name": "Transporter T5", "laneQuotes": [{"laneId": 9, "quote": 4500}, {"laneId": 10, "quote": 3500}]},
                {"id": 6, "name": "Transporter T6", "laneQuotes": [{"laneId": 1, "quote": 5500}, {"laneId": 2, "quote": 6200}]},
                {"id": 7, "name": "Transporter T7", "laneQuotes": [{"laneId": 3, "quote": 7500}, {"laneId": 4, "quote": 4500}]}
              ]
            }
            """;

    public static final String LIMIT_3 = """
            {"maxTransporters": 3}
            """;

    public static final String LIMIT_2 = """
            {"maxTransporters": 2}
            """;

    public static final String LIMIT_4 = """
            {"maxTransporters": 4}
            """;
}
