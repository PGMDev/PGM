import org.gradle.api.Project
import org.gradle.api.provider.Provider


fun Project.latestCommitHash(): Provider<String> {
    return runGitCommand(listOf("rev-parse", "--short", "HEAD"))
}

fun Project.runGitCommand(args: List<String>): Provider<String> {
    return providers.exec {
        commandLine("git")
        args(args)
    }.standardOutput.asText.map { it.trim() }
}