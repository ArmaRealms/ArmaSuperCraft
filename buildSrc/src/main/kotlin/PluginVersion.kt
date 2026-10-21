import org.gradle.api.provider.ProviderFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PluginVersion {
    private const val baseVersion = "4.0.0-SNAPSHOT"

    var version = baseVersion

    @JvmStatic
    fun updateVersion(providers: ProviderFactory) {
        version = try {
            getGitVersion(providers)
        } catch (exception: Git.DescribeException) {
            System.err.println("Unable to get version using Git: ${exception.message}")
            System.err.println("Using a timestamp instead.")

            getUtcVersion()
        }
    }

    private fun getGitVersion(providers: ProviderFactory): String {
        Git.describe(providers).run {
            val buildMetadata = mutableListOf<String>()

            if (commitsAhead > 0) {
                buildMetadata.add(commitsAhead.toString())
                buildMetadata.add('g' + commitHash.take(8))
            }

            if (dirty) {
                buildMetadata.add("dirty")
            }

            val metadataString = buildMetadata
                .takeIf { it.isNotEmpty() }
                ?.joinToString(".", prefix = "+")
                ?: ""

            return "$tag$metadataString"
        }
    }

    private fun getUtcVersion(): String {
        val time = DateTimeFormatter
            .ofPattern("yyyyMMddkkmmss")
            .withZone(ZoneId.of("UTC"))
            .format(Instant.now())

        return "$baseVersion+utc.$time"
    }
}