package webBrowsing

import org.openqa.selenium.WebElement
import java.lang.Integer.parseInt

/**
 * TODO
 */
class WebTableRowCellElement(private val cellElement: WebElement?) {
    fun asString(): String {
        return cellElement!!.text;
    }

    fun asInt(): Int {
        return parseInt(asString())
    }

}
