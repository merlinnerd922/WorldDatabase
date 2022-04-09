@file:Suppress("unused")

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
    val resource = resourceObj::class.java.getResource(fileName)
    return Paths.get(resource!!.toURI()).toFile()
}

/**
 * Write the provided string to the resource file provided by the given file name.
 */
fun writeToResourceFile(filename: String, jsonString: String?) {
    Paths.get("src", "main", "resources", filename).toFile().absoluteFile.writeText(jsonString!!);
}