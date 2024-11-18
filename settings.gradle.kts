rootProject.name = "pgm"

include(":util")
include(":platform-sportpaper")
include(":platform-modern")
include(":core")
include(":server")
project(":platform-sportpaper").projectDir = file("platform/platform-sportpaper")
project(":platform-modern").projectDir = file("platform/platform-modern")
