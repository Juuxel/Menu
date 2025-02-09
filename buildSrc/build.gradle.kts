repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
}

dependencies {
    implementation(gradleApi())
    implementation("net.fabricmc:fabric-filament:0.8.0")
    implementation("net.fabricmc:mapping-io:0.7.1")
}
