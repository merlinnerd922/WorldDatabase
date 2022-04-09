package webBrowsing

import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.pagefactory.ByChained
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
 * TODO
 */
fun WebDriver.findElementsStoreXPath(byChained: ByX): List<WebElementXPathed> {
    val findElements: MutableList<WebElement> = this.findElements(byChained.toBy())
    return findElements.withXPath(byChained);
}

/**
 * TODO
 */
private fun MutableList<WebElement>.withXPath(byChained: ByX) = map { WebElementXPathed(it, byChained) }

fun WebElementXPathed.findElements(byX: ByX): List<WebElementXPathed> {
    val findElements = this.webElement.findElements(byX.toBy())
    return findElements.map { WebElementXPathed(it, ByXChained(this.getXPath(), byX.getXPath())) }
}
