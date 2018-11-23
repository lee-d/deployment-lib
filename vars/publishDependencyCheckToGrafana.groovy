@NonCPS
def call(Map<String, Object> params) {
    def databaseName = params.databaseName
    def url = params.url
    def username = params.username
    def password = params.password
    def reportPath = params.reportPath ?: "${workspace}/target/dependency-check-report.xml"

    def influxdb = Jenkins.instance.getDescriptorByType(jenkinsci.plugins.influxdb.DescriptorImpl

    if (!influxdb.getTargets().any {it.description.equals(databaseName)}) {
        // Create target
        def target = new jenkinsci.plugins.influxdb.models.Target()

        // Set target details
        // Mandatory fields
        target.description = databaseName
        target.url = url
        target.username = username
        target.password = password
        target.database = databaseName

        influxdb.addTarget(target)
        influxdb.save()
    }

    def analysis = new XmlSlurper().parse(reportPath)
    def projectName = analysis.projectInfo.artifactID.text()

    def dependencyCheckMap = [:]
    def highestSeverityMap = [:]
    def highSeverityMap = [:]
    def normalSeverityMap = [:]
    def highestSeverityCount = 0
    def highSeverityCount = 0
    def normalSeverityCount = 0

    analysis.dependencies.dependency.each {
       highestSeverityCount = highestSeverityCount + it.vulnerabilities.vulnerability.count {
         it.severity.text().toLowerCase().equals('highest')
       }
       highSeverityCount = highSeverityCount + it.vulnerabilities.vulnerability.count {
          it.severity.text().toLowerCase().equals('high')
       }
       normalSeverityCount = normalSeverityCount + it.vulnerabilities.vulnerability.count {
        it.severity.text().toLowerCase().equals('medium')
      }

       highSeverityMap[projectName] = highSeverityCount
       highestSeverityMap[projectName] = highestSeverityCount
       normalSeverityMap[projectName] = normalSeverityCount
    }

    dependencyCheckMap['high-severity'] = highSeverityMap
    dependencyCheckMap['highest-severity'] = highestSeverityMap
    dependencyCheckMap['normal-severity'] = normalSeverityMap

    step([$class: 'InfluxDbPublisher', target: 'jenkins_codefluegel', selectedTarget: 'jenkins_codefluegel', customPrefix: null, customDataMap: dependencyCheckMap])
}
