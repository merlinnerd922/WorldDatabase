@file:Suppress("unused")

package webBrowsing

import org.openqa.selenium.WebElement
import java.lang.Integer.parseInt

/**
 * An abstraction of a table cell within a table.
 */
class WebTableRowCellElement(
    /**
     * The WebElement that this element wrapper wraps around.
     */
    private val cellElement: WebElement?) {

    /**
     * Return the contents of the cell as a String.
     */
    fun asString(): String {
        return cellElement!!.text;
    }

    /**
     * Return the contents of the cell as an int.
     */
    fun asInt(): Int {
        return parseInt(asString())
    }

}
