import org.gradle.api.provider.ProviderFactory

object Git {

    fun describe(providers: ProviderFactory): DescribeResult {
        val output = try {
            providers.exec { spec ->
                spec.commandLine("git", "describe", "--tags", "--long", "--abbrev=100", "--dirty")
            }.standardOutput.asText.get().trim()
        } catch (exception: Exception) {
            throw DescribeException("Unable to run command 'git describe'", exception)
        }

        val regex = Regex("""^v?(.*?)-(\d+)-g([0-9a-f]+)(-dirty)?$""")
        val match = regex.matchEntire(output)
            ?: throw DescribeException("Unexpected output from 'git describe': $output")

        val (tag, ahead, hash, dirty) = match.destructured

        return DescribeResult(
            tag = tag,
            commitsAhead = ahead.toInt(),
            commitHash = hash,
            dirty = dirty.isNotEmpty()
        )
    }

    class DescribeException(
        message: String,
        cause: Throwable? = null
    ) : Exception(message, cause)

    data class DescribeResult(
        val tag: String,
        val commitsAhead: Int,
        val commitHash: String,
        val dirty: Boolean
    )
}
