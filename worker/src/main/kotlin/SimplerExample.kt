fun <A, B, C> doToFirst(f: (A) -> C): (Pair<A,B>) -> Pair<C,B> {
    return fun(pair: Pair<A,B>): Pair<C,B> {
        return Pair(f(pair.first), pair.second)
    }
}

class CheckGamesFileEntries() {
    fun execute() {
        val gameIDs = listOf(1,2,3,4)
        fun extractGameId(filename: String) = 4
        val allIDsAndURLs: List<Pair<Int, String>> = listOf(Pair("1.xml", "abc")).map(doToFirst(::extractGameId))
        val unwantedGamesURLs = allIDsAndURLs.filter { it.first !in gameIDs }.map { it.second }
    }
}