package webBrowsing

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.pagefactory.ByChained

/**
 * TODO
 */
class WebTableRowElement(
    private val webElement: WebElementXPathed
) {

    /**
     * TODO
     */
    fun getColumn(columnIndex: Int): WebTableRowCellElement {



        val findElements = webElement.findElements(ByX("td[${columnIndex}]"))
        val xPath = webElement.getXPath();
        val cellElement: WebElementXPathed? = findElements.getOrNull(0);
        val xpath = cellElement!!.getXPath();
        return WebTableRowCellElement(cellElement.webElement);
    }

}

