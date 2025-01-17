package org.example

import groovy.xml.MarkupBuilder

class ResourceXOGGenerator {

    static String generateResourcesXML(List<Map> resourcesData) {
        StringWriter writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        println("Generating Resources XOG XML...")

        xml.NikuDataBus('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:noNamespaceSchemaLocation': '../xsd/nikuxog_resource.xsd') {
            xml.Header(version: '6.0.12', action: 'write', objectType: 'resource', externalSource: 'ORACLE-FINANCIAL')
            xml.Resources {
                resourcesData.each { resource ->
                    println("Creating XML for resource: ${resource.name}")
                    xml.Resource(resourceId: resource.id, isActive: resource.is_active.toString().toLowerCase(),
                            employmentType: "Employee", resourceType: "LABOR", externalId: "2323AAA") {
                        xml.PersonalInformation(lastName: resource.last_name, firstName: resource.first_name, emailAddress: resource.email)
                    }
                }
            }
        }

        println("Generated Resources XOG XML successfully.")
        return writer.toString()
    }
}
