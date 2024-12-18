dependencies {
    api(project(":jmccc"))
}

description = "jmccc yggdrasil authenticator"
tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "jmccc.yggdrasil.authenticator")
    }
}