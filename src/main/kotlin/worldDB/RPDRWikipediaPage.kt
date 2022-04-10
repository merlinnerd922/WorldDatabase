package worldDB

import webBrowsing.WebTableLocator
import WikipediaSite
import webBrowsing.getTableLocatorByChildCaptionPartial
import java.lang.Integer.parseInt

/**
 * A reference to a page containing information about contestants on a specific season of RPDR.
 */
class RPDRWikipediaPage(

    /**
     * The website containing this page.
     */
    internal val wikipediaSite: WikipediaSite
) {
    /**
     * Add all contestants currently listed on this page to the SQL DB, where the current season is season (seasonNumber).
     */
    fun addContestants(seasonNumber: Int) {

        // Retrieve a locator for the table we're extracting info from, by making use of its caption.
        val tableLocatorByChildCaptionPartial: WebTableLocator =
            wikipediaSite.webDriver.getTableLocatorByChildCaptionPartial("List of contestants on season $seasonNumber of ")

        // For each row representing a queen on the season:
        for (tableRowElement in tableLocatorByChildCaptionPartial) {

            // Extract the name of the queen here, so we can immediately print it out after.
            val queenName = tableRowElement.getTextByColNum(colNum = 1).also { println("Inserting $it") }

            // Insert info on the queen into the DB.
            RPDRSqlUtils.insertQueen(
                queenName,
                parseInt(tableRowElement.getTextByColNum(colNum = 2).removeSuffix("[a]")),
                tableRowElement.getTextByColNum(colNum = 3)
            )
        }
    }

}

