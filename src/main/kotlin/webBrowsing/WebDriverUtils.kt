@file:Suppress("unused", "RedundantVisibilityModifier")

package webBrowsing

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.Duration

/**
 * Return a locator for the table that has a caption that contains the given string.
 */
internal fun WebDriver.getTableLocatorByChildCaptionPartial(caption: String): WebTableLocator {
    val webTable = WebTableLocator()
    webTable.locator = ByX("//table[caption[contains(text(), \"$caption\")]]")
    webTable.driver = this;
    return webTable
}

/**
 * Return a Chrome WebDriver, dynamically downloaded.
 */
public fun getChromeDriver(implicitWaitSeconds: Int = 10) =
    WebDriverManager.chromedriver().create().withImplicitWaitSeconds(implicitWaitSeconds)

/**
 * Return this WebDriver with the provided implicit second wait.
 */
public fun WebDriver.withImplicitWaitSeconds(seconds: Int): WebDriver {
    this.manage().timeouts().implicitlyWait(Duration.ofSeconds(seconds.toLong()));
    return this;
}

/**
 * Given a ByX locator, return all WebElements on the page that correspond to that locator. Moreover, within the returned
 * list, store a reference to that element's locator.
 */
fun WebDriver.findElementsWithXPath(byChained: ByX): List<WebElementXPathed> {
    val findElements: MutableList<WebElement> = this.findElements(byChained.toBy())
    return findElements.withXPath(byChained);
}

/**
 * Return a wrapper around the provided list of WebElements, but with a stored reference to the XPath specified by
 * (byChained).
 */
private fun MutableList<WebElement>.withXPath(byChained: ByX) = map { WebElementXPathed(it, byChained) }

/**
 * Given a WebElement, return a list of WebElements that correspond to the elements located at the provided locator
 * (byX), relative to the WebElement.
 */
fun WebElementXPathed.findElements(byX: ByX): List<WebElementXPathed> {
    val findElements = this.webElement.findElements(byX.toBy())

    // Map the found WebElements to WebElement wrappers that expose the XPath of that WebElement.
    return findElements.map { WebElementXPathed(it, ByXChained(this.getXPath(), byX.getXPath())) }
}

/**
 * Given a locator that exposes its XPath value, return a list of WebElements at that locator that also contain
 * a reference to their XPath.
 */
fun WebDriver.findElements(byXChained: ByXChained): List<WebElementXPathed> {
    return this.findElements(byXChained.toBy()).map { WebElementXPathed(it, byXChained) }
}