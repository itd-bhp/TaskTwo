package org.example

import groovy.sql.Sql
import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import groovy.json.JsonBuilder
import groovy.xml.XmlParser

class ClarityService {

    static RestResponse sendRequest(String httpMethod, String endpoint, Map data = null) {
        DatabaseService.getDBConnection()  // Make sure DB connection is established
        ClarityRestClient rest = new ClarityRestClient("admin", DatabaseService.sql.getConnection(), "http://10.0.0.35:7080")
        def jsonData = data ? new JsonBuilder(data).toString() : null
        RestResponse response = null
        try {
            if (httpMethod == 'POST') {
                response = rest.POST(endpoint, jsonData)
            } else if (httpMethod == 'PATCH') {
                response = rest.PATCH(endpoint, jsonData)
            } else if (httpMethod == 'GET') {
                response = rest.GET(endpoint)
            }
            println("Response: ${response?.jsonMap()}")
        } catch (Exception e) {
            println("Caught exception: ${e.message}")
            e.printStackTrace()
        } finally {
            rest?.close()
        }
        return response
    }

    static void postProjectsWithTasks(List<Map> projects, List<Map> tasks) {
        projects.each { project ->
            def projectData = [
                    name          : project.project_name,
                    scheduleStart : project.start_date,
                    scheduleFinish: project.end_date,
                    createdDate   : project.created_date,
                    isActive      : project.is_active
            ]
            println("Posting project: ${projectData}")
            RestResponse projectResponse = sendRequest('POST', '/projects', projectData)
            if (projectResponse?.jsonMap()?._internalId) {
                def internalId = projectResponse.jsonMap()?._internalId
                println("Project created with internal ID: ${internalId}")
                def associatedTasks = tasks.findAll { it.project_id == project.id }
                println("Associated tasks for project ${project.name} (ID: ${project.id}): ${associatedTasks}")
                if (associatedTasks.isEmpty()) {
                    println("No tasks found for project ${project.name} (ID: ${project.id})")
                } else {
                    def postedTaskNames = []
                    associatedTasks.each { task ->
                        if (postedTaskNames.contains(task.name)) {
                            println("Skipping task '${task.name}' as it has already been posted.")
                        } else {
                            println("Task: ${task.name}, Status: ${task.status}")
                            def statusLookup = [
                                    'Not Started': [displayValue: 'Not Started', _type: 'lookup', id: '0'],   // Lookup object for 'Not Started'
                                    'In Progress': [displayValue: 'In Progress', _type: 'lookup', id: '1'],   // Lookup object for 'In Progress'
                                    'Completed'  : [displayValue: 'Completed', _type: 'lookup', id: '2']      // Lookup object for 'Completed'
                            ]
                            def validStatus = statusLookup[task.status]
                            if (validStatus) {
                                def taskData = [
                                        name     : task.name,
                                        _parentId: internalId,
                                        status   : validStatus,
                                        code     : task.id  // Only send the valid status object
                                ]
                                String taskEndpoint = "/projects/${internalId}/tasks"
                                println("Posting task to endpoint: ${taskEndpoint}, with data: ${taskData}")
                                RestResponse taskResponse = sendRequest('POST', taskEndpoint, taskData)
                                println("Task creation response: ${taskResponse?.jsonMap()}")
                                if (taskResponse) {
                                    postedTaskNames.add(task.name)
                                    println("Task created successfully: ${taskData}")
                                } else {
                                    println("Failed to create task: ${taskData}. Response: ${taskResponse?.jsonMap()}")
                                }
                            } else {
                                println("Invalid status '${task.status}' for task '${task.name}'. Skipping task creation.")
                            }
                        }
                    }
                }
            } else {
                println("Failed to create project: ${projectData}. Response: ${projectResponse?.jsonMap()}")
            }
        }
    }

    static void postTeamsWithResources(String xmlData) {
        def xmlParser = new XmlParser()
        def parsedXml = xmlParser.parseText(xmlData)
        parsedXml.'Projects'.'Project'.each { project ->
            def projectName = project.@name
            def projectId = project.@projectID
            String internalId = DatabaseService.getProjectInternalId(projectName)
            if (internalId) {
                project.'Tasks'.'Task'.each { task ->
                    task.'Assignments'.'TaskLabor'.each { assignment ->
                        def resourceCode = assignment.@resourceID
                        Map resourceDetails = DatabaseService.getResourceDetails(resourceCode)
                        if (resourceDetails) {
                            def teamData = [
                                    resource: resourceDetails.id
                            ]
                            RestResponse response = postTeamToClarity("/projects/${internalId}/teams", teamData)
                            if (response?.jsonMap()) {
                                println("Successfully added resource ${resourceDetails.code} to project ${projectId} team.")
                            } else {
                                if (response?.jsonMap()?.errorCode == 'projmgr.TEAM_RESOURCE_ALREADY_STAFFED') {
                                    def existingProjectId = response.jsonMap()?.errorMessage?.split(":")?.last()?.trim()
                                    if (existingProjectId != projectId) {
                                        println("Resource ${resourceDetails.code} is already assigned to another project ${existingProjectId}, proceeding with adding to current project.")
                                    } else {
                                        println("Resource ${resourceDetails.code} is already assigned to project ${projectId}. Skipping.")
                                    }
                                } else {
                                    println("Failed to add resource ${resourceDetails.code} to project ${projectId} team. Response: ${response?.jsonMap()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static RestResponse postTeamToClarity(String taskEndpoint, Map teamData) {
        RestResponse response = sendRequest('POST', taskEndpoint, teamData)
        if (response?.jsonMap()) {
            println("Successfully posted team data: ${teamData}")
        } else {
            println("Failed to post team data: ${teamData}. Response: ${response?.jsonMap()}")
        }
        return response
    }
}
