package org.saintqd.vineriumtraits

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class VineriumTraitsLoader : PluginLoader {

    override fun classloader(classpathBuilder: PluginClasspathBuilder) {

        val mavenResolver = MavenLibraryResolver()
        mavenResolver.addDependency(Dependency(DefaultArtifact("com.zaxxer:HikariCP:7.0.2"), null))
        mavenResolver.addDependency(Dependency(DefaultArtifact("com.mysql:mysql-connector-j:9.5.0"), null))
        mavenResolver.addDependency(Dependency(DefaultArtifact("io.github.classgraph:classgraph:4.8.184"), null))
        mavenResolver.addDependency(Dependency(DefaultArtifact("org.jdbi:jdbi3-core:3.53.0"), null))
        mavenResolver.addRepository(RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build())

        classpathBuilder.addLibrary(mavenResolver)
    }
}