package webBrowsing

import org.openqa.selenium.WebDriver

/**
 * The Wikipedia website.
 */
class WikipediaSite(internal val webDriver: WebDriver) {

    /**
     * Navigate to the Wikipedia page with the given (subURL).
     */
    fun goToPage(subURL: String) {
        webDriver.get("https://en.wikipedia.org/wiki/${subURL.replace("'", "%27")}")
    }

}

