package webBrowsing

import java.lang.Integer.parseInt

/**
 * TODO
 */
class RPDRWikipediaPage(

    /**
     * TODO
     */
    private val wikipediaSite: WikipediaSite
) {
    /**
     * TODO
     */
    fun addContestants(season: Int) {

        // Retrieve a locator for the table we're extracting info from, by making use of its caption.
        val tableLocatorByChildCaptionPartial: WebTableLocator =
            wikipediaSite.webDriver.getTableLocatorByChildCaptionPartial("List of contestants on season $season of ")

        // TODO
        for (tableRowElement in tableLocatorByChildCaptionPartial) {
            val queenName = tableRowElement.getTextByColNum(colNum = 1)
            RPDRSqlUtils.insertQueen(
                queenName,
                parseInt(tableRowElement.getTextByColNum(colNum = 2).removeSuffix("[a]")),
                tableRowElement.getTextByColNum(colNum = 3)
            )
            println("Inserting $queenName");
        }
    }

}

