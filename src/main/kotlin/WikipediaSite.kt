import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/**
 * The Wikipedia website.
 */
class WikipediaSite(internal val webDriver: WebDriver) {

    /**
     * Navigate to the Wikipedia page with the given (subURL).
     */
    fun goToPage(subURL: String) {
        webDriver.get("https://en.wikipedia.org/wiki/${subURL.replace("'", "%27")}")
        webDriver.waitForPageToLoad();
    }

}

private fun WebDriver.waitForPageToLoad() {
    WebDriverWait(this, Duration.ofSeconds(30)).until { webDriver: WebDriver ->
        (webDriver as JavascriptExecutor).executeScript(
            "return document.readyState"
        ) == "complete"
    }
}

