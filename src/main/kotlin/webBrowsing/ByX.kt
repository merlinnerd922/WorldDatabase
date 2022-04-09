package webBrowsing

import org.openqa.selenium.By

open class ByX(val internalXPath: String) {



    fun getXPath(): String {
        return internalXPath;
    }

    fun toBy(): By? {
        return By.xpath(internalXPath);
    }

    companion object {


    }

}
