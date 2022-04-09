@file:Suppress("unused")

package utils

import java.io.File
import java.nio.file.Paths

/**
 * An object instance that exists for the purpose of being able to access resources.
 */
public val resourceObj = object {}

/**
 * Return the file representing the resource (fileName).
 */
fun getResource(fileName: String) : File {
    val resource = resourceObj::class.java.getResource("/$fileName")
    return Paths.get(resource!!.toURI()).toFile()
}

/**
 * Write the provided string to the resource file provided by the given file name.
 */
fun writeToMainResourceFile(filename: String, jsonString: String?) {
    Paths.get("src", "exe.example.main", "resources", filename).toFile().absoluteFile.writeText(jsonString!!);
}

/**
 * Return the text stored in the resource file with the provided path.
 */
fun getResourceText(filePath: String): String = getResource(filePath).readText()