package exe.example

import WikipediaSite
import org.openqa.selenium.WebDriver
import utils.getResource
import utils.sql.MySQLDB
import utils.sql.deleteContentsThenSecure
import webBrowsing.WebTableLocator
import webBrowsing.getChromeDriver
import webBrowsing.getTableLocatorByChildCaptionPartial
import worldDB.RPDRSqlUtils
import worldDB.RPDRWikipediaPage
import java.io.File
import java.lang.Integer.parseInt

/**
 * An entry point for extracting information about RPDR drag queens and adding them to a DB.
 */
fun main() {
    // Clear the DB of queens first.
    MySQLDB.MY_SQL_DB_CONNECTION.deleteContentsThenSecure(schema = "people", table = "people")
    // Create a new WebDriver and navigate to each of the individual seasons' pages to extract info.
    val webDriver: WebDriver = getChromeDriver(implicitWaitSeconds = 5)
    val wikipediaSite = WikipediaSite(webDriver)
    val rpdrSeasonPage = RPDRWikipediaPage(wikipediaSite)
    val resource = getResource("queenNames.txt")

    for (i in (1..14)) {
        wikipediaSite.goToPage("RuPaul's_Drag_Race_(season_$i)")

        // Retrieve a locator for the table we're extracting info from, by making use of its caption.
        val tableLocatorByChildCaptionPartial: WebTableLocator =
            rpdrSeasonPage.wikipediaSite.webDriver.getTableLocatorByChildCaptionPartial("List of contestants on season $i of ")

        var lines = mutableListOf<String>();
        // For each row representing a queen on the season:
        for (tableRowElement in tableLocatorByChildCaptionPartial) {

            // Extract the name of the queen here, so we can immediately print it out after.
            val queenName = tableRowElement.getTextByColNum(colNum = 1).also { println("Inserting $it") }

            lines.add(queenName);
        }

        resource.appendLines(lines);
    }
}

private fun File.appendLines(lines: MutableList<String>) {

    this.appendText(lines.joinToString(separator = System.lineSeparator()));
}

/**
 * TODO
 */
private fun File.writeLines(lines: MutableList<String>) {
    this.writeText(lines.joinToString(separator = System.lineSeparator()));
}


