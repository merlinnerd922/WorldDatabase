package webBrowsing

import org.openqa.selenium.WebElement

/**
 * A wrapper around the WebElement class that has a reference to the locator used to find it.
 */
class WebElementXPathed(val webElement: WebElement, private val byChained: ByX) {

    /**
     * Return the XPath of this WebElement.
     */
    fun getXPath(): String {
        return byChained.getXPath();
    }
}
