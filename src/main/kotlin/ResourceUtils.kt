import java.io.File
import java.nio.file.Paths

/**
 * TODO
 */
public val resourceObj = object {}

fun getResource(fileName: String) : File {
    val resource = resourceObj::class.java.getResource(fileName)
    return Paths.get(resource!!.toURI()).toFile()
}