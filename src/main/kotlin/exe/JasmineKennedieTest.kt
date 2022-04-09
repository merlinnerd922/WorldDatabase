package exe

import utils.sql.MySQLDB
import webBrowsing.RPDRWikipediaPage
import webBrowsing.WikipediaSite
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.WebDriver
import java.sql.Connection
import java.time.Duration

/**
 * TODO
 * An entry point for extracting information about the drag queen Jasmine Kennedie from an article.
 */
fun main() {

    val webDriver: WebDriver = WebDriverManager.chromedriver().create().withImplicitWaitSeconds(10);
    val wikipediaSite = WikipediaSite(webDriver);
    MySQLDB.MY_SQL_DB_CONNECTION.deleteContents(schema = "people", table = "people");

    for (i in (1..14)) {

        wikipediaSite.goToPage("RuPaul's_Drag_Race_(season_$i)")
        val rpdrSeasonPage = RPDRWikipediaPage(wikipediaSite);
        rpdrSeasonPage.insertContestants(i);
    }

}

private fun WebDriver.withImplicitWaitSeconds(seconds: Int): WebDriver {
    this.manage().timeouts().implicitlyWait(Duration.ofSeconds(seconds.toLong()));
    return this;
}

public fun Connection.deleteContents(schema: String, table: String) {
    this.prepareStatement("DELETE FROM $schema.$table");
}


