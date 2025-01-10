package org.example

import de.itdesign.clarity.rest.RestResponse
import de.itdesign.clarity.rest.ClarityRestClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.sql.Sql

import java.sql.Connection
import java.sql.DriverManager

class Sample {

    static Connection connect() {
        String dbUrl = "jdbc:oracle:thin:@//10.0.0.35:11521/clarity"
        String dbUser = "niku"
        String dbPassword = "niku"
        Connection connection = null

        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
            println "Connected to the database successfully!"
        } catch (Exception e) {
            e.printStackTrace()
        }
        return connection
    }

    static List readJsonFromResources(String filename) {
        def jsonFile = Sample.getClassLoader().getResource(filename)
        def data = []
        if (jsonFile) {
            def jsonSlurper = new JsonSlurper()
            def parsedJson = jsonSlurper.parse(jsonFile)
            data = parsedJson.projects ?: parsedJson.tasks
            println "Parsed JSON: ${data}"
        } else {
            println "File not found in resources!"
        }
        return data
    }

    static RestResponse Request(String httpMethod, String endpoint, Map data = null) {
        Connection conn = connect()
        Sql sql = new Sql(conn)
        ClarityRestClient rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.35:7080")
        def jsonData = data ? new JsonBuilder(data).toString() : null
        RestResponse response
        try {
            if (httpMethod == 'POST') {
                response = rest.POST(endpoint, jsonData)
            } else if (httpMethod == 'PATCH') {
                response = rest.PATCH(endpoint, jsonData)
            } else if (httpMethod == 'GET') {
                response = rest.GET(endpoint)
            }
            println "Response: ${response?.jsonMap()}"
        } catch (Exception e) {
            println "Caught exception: ${e.message}"
            e.printStackTrace()
        } finally {
            rest?.close()
        }
        return response
    }

    static void postProjects(List<Map> projects, List<Map> tasklist) {
        projects.each { project ->
            // Prepare the JSON data for the project
            def projectData = [
                    name          : project.project_name,
                    scheduleStart : project.start_date,
                    scheduleFinish: project.end_date,
                    createdDate   : project.created_date,
                    isActive      : true
            ]
            println(projectData)
            println "Posting project: ${projectData}"
            RestResponse projectResponse = Request('POST', '/projects', projectData)
            if (projectResponse?.jsonMap()?._internalId) {
                def internalId = projectResponse.jsonMap()?._internalId
                println "Project created with internal ID: ${internalId}"

                // Fix the task filtering condition
                def tasks = tasklist.findAll { task -> task.project_name == project.project_name }
                println "List of tasks of a project ${project.project_name}: ${tasks}"

                if (tasks.isEmpty()) {
                    println "No tasks found for project ${project.project_name}"
                } else {
                    tasks.each { task ->
                        println "Task: ${task.name}, Status: ${task.status}"
                        def taskData = [
                                name     : task.name,
                                _parentId: internalId,
                                status   : task.status
                        ]
                        String taskEndpoint = "/projects/${internalId}/tasks"
                        println "Posting task to endpoint: ${taskEndpoint}, with data: ${taskData}"

                        RestResponse taskResponse = Request('POST', taskEndpoint, taskData)

                        println "Task creation response: ${taskResponse?.jsonMap()}"
                        if (taskResponse) {
                            println "Task created successfully: ${taskData}"
                        } else {
                            println "Failed to create task: ${taskData}. Response: ${taskResponse?.jsonMap()}"
                        }
                    }
                }
            } else {
                println "Failed to create project: ${projectData}. Response: ${projectResponse?.jsonMap()}"
            }
        }
    }

    static void main(String[] args) {
        List<Map> projects = readJsonFromResources('projects.json')
        List<Map> tasks = readJsonFromResources('tasks.json')
        connect()
        postProjects(projects, tasks)
    }
}
