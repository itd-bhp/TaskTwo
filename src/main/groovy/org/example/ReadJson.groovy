package org.example

import groovy.json.JsonSlurper

class ReadJson {
    List<Map> readJSON(String jsonFile, String key) {
        validateJSONFilePath(jsonFile)
        InputStream inputStream = locateJSONFile(jsonFile)
        if (inputStream == null) {
            println "ERROR: File not found: ${jsonFile}"
            return []
        }
        String content = readJSONContent(inputStream)
        validateJSONContent(content)
        return parseJSONToMap(content, key)
    }

    private void validateJSONFilePath(String jsonFile) {
        if (!jsonFile || jsonFile.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON file path is missing or empty.")
        }
        println "INFO: JSON file path validated: ${jsonFile}"
    }

    private InputStream locateJSONFile(String jsonFile) {
        println "INFO: Locating JSON file: ${jsonFile}"
        InputStream inputStream = ReadJson.class.classLoader.getResourceAsStream(jsonFile)
        if (inputStream != null) {
            println "INFO: JSON file found: ${jsonFile}"
            return inputStream
        }
        println "WARN: JSON file not found in resources: ${jsonFile}"
        return null
    }

    private String readJSONContent(InputStream inputStream) {
        println "INFO: Reading content from input stream."
        String content = inputStream.text // Converting the InputStream to a String
        if (content.isEmpty()) {
            throw new IllegalArgumentException("JSON file is empty.")
        }
        println "DEBUG: Read content from input stream."
        return content
    }

    private void validateJSONContent(String content) {
        if (!content) {
            throw new IllegalArgumentException("JSON content is empty.")
        }
    }

    private List<Map> parseJSONToMap(String content, String key) {
        try {
            def json = new JsonSlurper().parseText(content)
            println "parsed content: ${json.getClass()}"

            // Check if the required field (key) exists and is a list
            if (!(json."$key" instanceof List)) {
                throw new IllegalArgumentException("Expected JSON to have a '${key}' key that is an array of objects.")
            }

            // Return the content associated with the provided key
            return json."$key"
        } catch (Exception e) {
            println "ERROR: Error parsing JSON content: ${e.message}"
            throw e
        }
    }

    List<Map> projectJson() {
        List<Map> projects = readJSON("project.json", "projects")
        return projects
    }

    List<Map> taskJson() {
        List<Map> tasks = readJSON("task.json", "tasks")
        return tasks
    }

    List<Map> resourceJson() {
        List<Map> resources = readJSON("resources.json", "resources")
        return resources
    }

    List<Map> assignJson() {
        List<Map> assigns = readJSON("assignments.json", "assignments")
        return assigns
    }
}

