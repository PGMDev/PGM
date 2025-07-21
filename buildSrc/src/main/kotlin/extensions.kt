import org.gradle.api.Project


fun Project.latestCommitHash(): String {
    return runGitCommand(listOf("rev-parse", "--short", "HEAD"))
}

fun Project.runGitCommand(args: List<String>): String {
    return providers.exec {
        commandLine("git")
        args(args)
    }.standardOutput.asText.get().trim()
}