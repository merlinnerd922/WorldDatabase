package webBrowsing

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.lang.Integer.parseInt

/**
 * TODO
 */
class RPDRWikipediaPage(
    val wikipediaSite: WikipediaSite
) {
    fun insertContestants(season: Int) {
        val tableLocatorByChildCaptionPartial =
            wikipediaSite.webDriver.getTableLocatorByChildCaptionPartial("List of contestants on season $season of ")
        for (tableRowElement in tableLocatorByChildCaptionPartial) {
            val queenName = tableRowElement.getColumn(1).asString()
            RPDRSqlUtils.insertQueen(
                queenName,
                parseInt(tableRowElement.getColumn(2).asString().removeSuffix("[a]")),
                tableRowElement.getColumn(3).asString()
            )
            println("Inserting $queenName");
        }
    }

}

private fun WebDriver.getTableLocatorByChildCaptionPartial(caption: String): WebTableLocator {
    val webTable = WebTableLocator()
    webTable.locator = By.xpath("//table[caption[contains(text(), \"$caption\")]]")
    webTable.driver = this;
    return webTable
}
