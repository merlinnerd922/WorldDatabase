package webBrowsing

import org.openqa.selenium.By
import org.openqa.selenium.WebElement

/**
 * TODO
 */
class WebTableRowElement(
    private val webElement: WebElement
) {
    fun getColumn(columnIndex: Int): WebTableRowCellElement {
        return WebTableRowCellElement(webElement.findElement(By.xpath("td[${columnIndex}]")))
    }

}
