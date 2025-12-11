package fansirsqi.xposed.sesame.newutil

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object UrlHelper {
    /**
    对URL编码的字符串进行解码

    @param encodedString 需要解码的URL编码字符串
    @return 解码后的字符串，如果解码失败则返回原始字符串
     */
    fun customUrlDecode(encodedString: String): String {
        return try {
            URLDecoder.decode(encodedString, StandardCharsets.UTF_8.name())
        } catch (e: UnsupportedEncodingException) {
            encodedString  // 如果无法解码，返回原始字符串
        }
    }

}

