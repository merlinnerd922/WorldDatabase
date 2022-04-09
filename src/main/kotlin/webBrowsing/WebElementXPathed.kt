package webBrowsing

import org.openqa.selenium.WebElement

class WebElementXPathed(val webElement: WebElement, val byChained: ByX) {
    fun getXPath(): String {
        return byChained.getXPath();
    }


}
