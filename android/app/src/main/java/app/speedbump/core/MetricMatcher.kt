package app.speedbump.core

data class MetricHit(val kind: MetricKind, val bounds: Box)

/** Finds Instagram metric counts. Whole-string matching leaves captions and comments alone. */
object MetricMatcher {
    private const val COUNT = """\d[\d.,\s]*[KkMmBb]?"""
    private val BARE_COUNT = Regex("""^$COUNT$""")
    private val PATTERNS = listOf(
        MetricKind.LIKES to Regex("""^$COUNT\s*likes?$|^liked by .+|^like number is .+|^likes?:?\s*$COUNT$""", RegexOption.IGNORE_CASE),
        MetricKind.VIEWS to Regex("""^$COUNT\s*(views?|plays?)$|^view count.*""", RegexOption.IGNORE_CASE),
        MetricKind.FOLLOWERS to Regex("""^$COUNT\s*(followers?|following)$""", RegexOption.IGNORE_CASE),
    )
    private val LABELS = mapOf(
        "like" to MetricKind.LIKES,
        "likes" to MetricKind.LIKES,
        "view" to MetricKind.VIEWS,
        "views" to MetricKind.VIEWS,
        "plays" to MetricKind.VIEWS,
        "followers" to MetricKind.FOLLOWERS,
        "follower" to MetricKind.FOLLOWERS,
        "following" to MetricKind.FOLLOWERS,
    )

    fun classify(text: String?): MetricKind? {
        val value = text?.trim().orEmpty()
        if (value.isEmpty()) return null
        return PATTERNS.firstOrNull { it.second.containsMatchIn(value) }?.first
    }

    fun find(root: UiNode, wanted: Set<MetricKind>): List<MetricHit> {
        if (wanted.isEmpty()) return emptyList()
        val hits = mutableListOf<MetricHit>()
        collect(root, wanted, hits)
        return hits
    }

    private fun collect(node: UiNode, wanted: Set<MetricKind>, hits: MutableList<MetricHit>) {
        if (!node.visible) return
        // A clickable node described by its count is often also the button itself, and covering
        // that would hide the heart icon, so description-only matches skip clickable nodes.
        val kind = classify(node.text) ?: if (node.clickable) null else classify(node.desc)
        if (kind != null && kind in wanted && node.bounds.area > 0) {
            hits += MetricHit(kind, node.bounds)
            return
        }
        // Some screens show the number and its label as two neighbouring views.
        val visibleChildren = node.children.filter { it.visible }
        val labelKinds = visibleChildren.mapNotNull { child ->
            LABELS[(child.text ?: child.desc)?.trim()?.lowercase()]
        }.distinct()
        if (labelKinds.size == 1 && labelKinds[0] in wanted) {
            visibleChildren.filter { BARE_COUNT.matches(it.text?.trim().orEmpty()) && it.bounds.area > 0 }
                .forEach { hits += MetricHit(labelKinds[0], it.bounds) }
        }
        node.children.forEach { collect(it, wanted, hits) }
    }
}
