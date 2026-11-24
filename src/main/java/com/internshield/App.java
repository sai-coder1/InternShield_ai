
package com.internshield;

import com.internshield.search.OctenSearchClient;
import com.fasterxml.jackson.databind.JsonNode;

public class App {

    public static void main(String[] args) {

        try {
            OctenSearchClient client = new OctenSearchClient();

            String query = "internship registration fees and training fees";

            System.out.println("Searching Octen...");
            JsonNode results = client.search(query);

            System.out.println("\nSearch Results:\n");

            for (JsonNode result : results) {

                System.out.println("Title: " +
                        result.path("title").asText());

                System.out.println("URL: " +
                        result.path("url").asText());

                System.out.println("Highlight: " +
                        result.path("highlight").asText());

                System.out.println("--------------------------------");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
