package webBrowsing

/**
 * An abstraction of a row in a table on a web page.
 */
class WebTableRowElement(

    /**
     * The WebElement that this row object wraps around.
     */
    private val webElement: WebElementXPathed
) {

    /**
     * Return the table cell element at the (columnIndex)th column within this row. Moreover, assert it exists.
     */
    private fun getColumn(columnIndex: Int): WebTableRowCellElement {
        val findElements = webElement.findElements(ByX("td[${columnIndex}]"))
        val cellElement: WebElementXPathed? = findElements.getOrNull(0);
        return WebTableRowCellElement(cellElement!!.webElement);
    }

    /**
     * Return the text within the (colNum)th column within the row.
     */
    public fun getTextByColNum(colNum: Int) = getColumn(colNum).asString()
}