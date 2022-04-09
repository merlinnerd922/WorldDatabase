package webBrowsing

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.pagefactory.ByChained

/**
 * TODO
 */
class WebTableLocator {

    var driver: WebDriver?=null
    var locator: ByX? = null;

    public operator fun iterator() : Iterator<WebTableRowElement> {

        val mutableList = mutableListOf<WebTableRowElement>();
        val findElements: List<WebTableRowElement> = driver!!.findElements(ByXChained(locator!!.getXPath(), "tbody/tr")).map { WebTableRowElement(it) };
        return findElements.iterator();

    }
}

/**
 * TODO
 */
private fun WebDriver.findElements(byXChained: ByXChained): List<WebElementXPathed> {
    return this.findElements(byXChained.toBy()).map { WebElementXPathed(it, byXChained) }
}

/**
 * TODO
 */
public fun WebTableRowElement.getTextByColNum(colNum: Int) = getColumn(colNum).asString()