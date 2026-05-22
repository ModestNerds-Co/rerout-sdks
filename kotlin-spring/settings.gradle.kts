rootProject.name = "rerout-spring-boot-starter"

// Resolve co.rerout:rerout-kotlin from the sibling base SDK during development.
// On release the published Maven Central artifact is used instead.
includeBuild("../kotlin")
