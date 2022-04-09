package utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class URLUtils {
}

public fun encodeValue(value: String): String? {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}