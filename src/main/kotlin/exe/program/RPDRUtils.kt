package exe.program

import org.openqa.selenium.WebDriver
import utils.sql.MySQLDB
import utils.sql.deleteContentsThenSecure
import worldDB.RPDRWikipediaPage
import WikipediaSite
import webBrowsing.getChromeDriver

/**
 * After clearing the people.people DB, navigate to each of the Wikipedia pages for each of the seasons of RPDR and
 * extract info on each queen, and insert that info into a DB.
 */
public fun crawlAndAddRPDRContestants() {

    // Clear the DB of queens first.
    MySQLDB.MY_SQL_DB_CONNECTION.deleteContentsThenSecure(schema = "people", table = "people");

    // Create a new WebDriver and navigate to each of the individual seasons' pages to extract info.
    val webDriver: WebDriver = getChromeDriver(implicitWaitSeconds = 5);
    val wikipediaSite = WikipediaSite(webDriver);
    val rpdrSeasonPage = RPDRWikipediaPage(wikipediaSite);

    for (i in (1..14)) {
        wikipediaSite.goToPage("RuPaul's_Drag_Race_(season_$i)")
        rpdrSeasonPage.addContestants(i);
    }
}