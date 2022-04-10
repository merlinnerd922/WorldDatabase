package webBrowsing

import org.openqa.selenium.By

/**
 * A wrapper around a By locator that exposes the XPath of this locator, which also is a mandatory field.
 */
open class ByX(private val internalXPath: String) {

    /**
     * Return the internal XPath of this locator.
     */
    fun getXPath(): String {
        return internalXPath;
    }

    /**
     * Return a By object wrapped around this locator's path.
     */
    fun toBy(): By {
        return By.xpath(internalXPath);
    }

}
