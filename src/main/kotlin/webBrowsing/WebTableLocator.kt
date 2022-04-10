package webBrowsing

import org.openqa.selenium.WebDriver

/**
 * A reference a table within a web page.
 */
class WebTableLocator {

    /**
     * The WebDriver that was used to create this locator.
     */
    var driver: WebDriver?=null

    /**
     * The locator containing the XPath of this element.
     */
    var locator: ByX? = null;

    /**
     * Return an iterator over all the row elements within this table.
     */
    operator fun iterator() : Iterator<WebTableRowElement> {
        val foundElements = driver!!.findElements(ByXChained(locator!!.getXPath(), "tbody/tr"))
        val asTableRowElements: List<WebTableRowElement> = foundElements.map { WebTableRowElement(it) };
        return asTableRowElements.iterator();

    }


}

