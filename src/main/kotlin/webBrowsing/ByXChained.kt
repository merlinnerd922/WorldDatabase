package webBrowsing

/**
 * A wrapper around a ByChained object that exposes the internal XPath.
 */
class ByXChained(vararg args : String) : ByX(args.joinToString(separator = "/"))
