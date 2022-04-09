package webBrowsing

import org.openqa.selenium.WebDriver

/**
 * TODO
 */
class WikipediaSite(internal val webDriver: WebDriver) {
    fun goToPage(subURL: String) {
        webDriver.get("https://en.wikipedia.org/wiki/${subURL.replace("'", "%27")}")
    }

}

