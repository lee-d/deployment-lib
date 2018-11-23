def call(Map<String, Object> params) {
    def databaseName = params.databaseName
    def influxdb = Jenkins.instance.getDescriptorByType(jenkinsci.plugins.influxdb.DescriptorImpl)

    if (!influxdb.getTargets().any { (it.description == databaseName) }) {
        def target = new jenkinsci.plugins.influxdb.models.Target()
        target.description = databaseName
        target.url = params.url
        target.username = params.username
        target.password = params.password
        target.database = databaseName
        influxdb.addTarget(target)
        influxdb.save()
    }

    def reportPath = params.reportPath ?: "${workspace}/target/dependency-check-report.xml"
    def analysis = new XmlSlurper().parse(reportPath)
    String projectName = analysis.projectInfo.artifactID.text()
    def dependencyCheckMap = [:]
    def highestSeverityMap = [:]
    def highSeverityMap = [:]
    int highestSeverityCount = 0
    int highSeverityCount = 0

    analysis.dependencies.dependency.each {
        highestSeverityCount = highestSeverityCount + it.vulnerabilities.vulnerability.count {
            it.severity.text().toLowerCase().equals('highest')
        }
        highSeverityCount = highSeverityCount + it.vulnerabilities.vulnerability.count {
            it.severity.text().toLowerCase().equals('high')
        }

        highSeverityMap[projectName] = highSeverityCount
        highestSeverityMap[projectName] = highestSeverityCount
    }

    dependencyCheckMap['high-severity'] = highSeverityMap
    dependencyCheckMap['highest-severity'] = highestSeverityMap

    step([$class: 'InfluxDbPublisher', target: databaseName, selectedTarget: databaseName, customPrefix: null, customDataMap: dependencyCheckMap])
}
