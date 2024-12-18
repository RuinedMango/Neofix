dependencies {
    api(project(":jmccc-yggdrasil-authenticator"))
}

description = "jmccc-mojang-api"
tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "jmccc-mojang-api")
    }
}