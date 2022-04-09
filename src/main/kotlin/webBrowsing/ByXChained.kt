package webBrowsing

import org.openqa.selenium.By

class ByXChained(vararg args : String) : ByX(args.joinToString(separator = "/")) {

}
