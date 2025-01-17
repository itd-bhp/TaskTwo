package org.example

import groovy.xml.MarkupBuilder

class AssignmentXOGGenerator {
    static boolean isDuplicateAssignment(List<Map> existingAssignments, Map assignment) {
        println("Checking if assignment is duplicate: ${assignment}")
        return existingAssignments.any { existingAssignment ->
            existingAssignment.resource_id == assignment.resource_id && existingAssignment.task_id == assignment.task_id
        }
    }

    static String generateAssignmentXOGXML(
            List<Map> resourcesData,
            List<Map> assignmentsData,
            List<Map> tasksData,
            List<Map> projectsData,
            List<Map> filteredProjects) {
        StringWriter writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        println("Generating Assignment XOG XML...")
        xml.NikuDataBus('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:noNamespaceSchemaLocation': '../xsd/nikuxog_project.xsd') {
            xml.Header(action: 'write', externalSource: 'NIKU', objectType: 'project', version: '7.1.0.3023')
            xml.Projects {
                projectsData.each { project ->
                    def matchingProjectFromClarity = filteredProjects.find { it.name == project.project_name }
                    if (matchingProjectFromClarity) {
                        xml.Project(projectID: matchingProjectFromClarity.code, name: project.project_name) {
                            xml.Tasks {
                                def processedTasks = new HashSet()
                                tasksData.findAll { it.project_id == project.id }.each { task ->
                                    def taskAssignments = assignmentsData.findAll { it.task_id == task.id }
                                    if (taskAssignments) {
                                        if (!processedTasks.contains(task.name)) {
                                            processedTasks.add(task.name)
                                            println("Generating XML for task: ${task.name}")

                                            xml.Task(taskID: task.id, outlineLevel: "1", name: task.name) {
                                                xml.Assignments {
                                                    def existingAssignments = []

                                                    taskAssignments.each { assignment ->
                                                        if (isDuplicateAssignment(existingAssignments, assignment)) {
                                                            println("Duplicate assignment skipped: ${assignment}")
                                                            return
                                                        }
                                                        existingAssignments << assignment
                                                        println("Processing assignment for task: ${task.name}, Assignment: ${assignment}")
                                                        def resource = resourcesData.find { it.id.toString().trim() == assignment.resource_id.toString().trim() }
                                                        if (resource) {
                                                            println("Found matching resource for assignment: ${assignment.resource_id} -> ${resource.id}")
                                                            xml.TaskLabor(
                                                                    actualWork: assignment.actuals ?: "0",
                                                                    baselineWork: "0",
                                                                    remainingWork: assignment.etc ?: "0",
                                                                    resourceID: resource.id) {
                                                                xml.CustomInformation()
                                                            }
                                                        } else {
                                                            println("No matching resource for assignment: ${assignment}")
                                                        }
                                                    }
                                                }
                                                xml.CustomInformation()
                                            }
                                        }
                                    } else {
                                        println("No assignments found for task: ${task.name}")
                                    }
                                }
                            }
                            xml.Dependencies()
                            xml.CustomInformation()
                            xml.OBSAssocs()
                        }
                    }
                }
            }
        }
        println("Generated Assignment XOG XML successfully.")
        return writer.toString()
    }
}
