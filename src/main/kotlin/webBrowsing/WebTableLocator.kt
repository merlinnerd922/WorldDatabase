package webBrowsing

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.pagefactory.ByChained

/**
 * TODO
 */
class WebTableLocator {

    var driver: WebDriver?=null
    var locator: By? = null;

    public operator fun iterator() : Iterator<WebTableRowElement> {

        val mutableList = mutableListOf<WebTableRowElement>();
        val findElements: List<WebTableRowElement> = driver!!.findElements(ByChained(locator, By.xpath("tbody/tr"))).map { WebTableRowElement(it) };
        return findElements.iterator();

    }
}
