import org.ajoberstar.grgit.Grgit
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class GitBuildInfoValueSource : ValueSource<Map<String, String>, GitBuildInfoValueSource.Params> {
    interface Params : ValueSourceParameters {
        val projectDir: Property<String>
    }

    override fun obtain(): Map<String, String> =
        Grgit.open(mapOf("currentDir" to parameters.projectDir.get())).use { git ->
            mapOf(
                "git.branch" to git.branch.current().name,
                "git.commit.id" to git.head().abbreviatedId,
                "git.commit.time" to git.head().dateTime.toString()
            )
        }
}
