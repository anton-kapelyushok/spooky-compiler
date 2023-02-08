@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package booking.spooky


import com.sun.source.tree.*
import com.sun.source.util.JavacTask
import com.sun.source.util.Trees
import com.sun.tools.javac.api.BasicJavacTask
import com.sun.tools.javac.api.JavacTrees
import com.sun.tools.javac.util.Context
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.tools.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

// JVM args
// -ea --add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED
// Program arguments
// -cp booking-lib/build/classes/java/main/ -java_out java_out -perl_out perl_out -input my-lib/src/main/java
fun main(args: Array<String>) {
    val options = HashMap<String, String>()

    if (args.isEmpty()) {
        println("Usage example: ./spooky.sh -cp <path1>:<path2> -java_out <java_out> -perl_out <perl_out> -input <java_in>")
        return
    }

    var i = 0
    while (i < args.size) {
        when {
            args[i] == "-cp" -> options["cp"] = args[++i]
            args[i] == "-java_out" -> options["java_out"] = args[++i]
            args[i] == "-perl_out" -> options["perl_out"] = args[++i]
            args[i] == "-input" -> options["input"] = args[++i]
        }
        i++
    }

    options["cp"] =
        ((options["cp"] ?: "").split(":") + listOf("lombok.jar", "perl-lib/build/classes/java/main/")).joinToString(":")

    options["java_out"] = options["java_out"] ?: "."
    options["perl_out"] = options["perl_out"] ?: "."
    options["input"]!!


    val javac = ToolProvider.getSystemJavaCompiler()
    val fileManager = javac.getStandardFileManager(null, null, null)

    val inputDirs = options["input"]!!.split(":").map { Path.of(it) }
    val fileObjects = fileManager.getJavaFileObjectsForDirectories(*inputDirs.toTypedArray())

    val task = javac.getTask(
        null, fileManager, { diagnostic ->
            println("diagnostic")
            if ((diagnostic.kind == Diagnostic.Kind.ERROR)) {
                error(diagnostic)
            } else {
                println(diagnostic)
            }
        }, listOf(
            "-cp", options["cp"]!!,
            "-d", options["java_out"]!!,
        ), null, fileObjects
    ) as JavacTask


    val cus = task.parse()
    task.analyze()


    val trees = Trees.instance(task) as JavacTrees
    val generated = generateCode(trees, cus.toList(), task)
    val emitted = emitCode(generated)

    Path.of(options["perl_out"]!!).toFile().deleteRecursively()

    for (f in emitted) {
        val path = Path.of(options["perl_out"]!!, f.relativePath)
        path.parent.toFile().mkdirs()
        path.writeText(f.content)
    }

    task.generate()
}


fun StandardJavaFileManager.getJavaFileObjectsForDirectories(vararg dirs: Path): List<JavaFileObject> {
    val files = mutableListOf<Path>()

    dirs.forEach { dir ->

        Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file.absolutePathString().endsWith("java")) files.add(file)
                return FileVisitResult.CONTINUE
            }
        })
    }

    return this.getJavaFileObjectsFromPaths(files).toList()
}
