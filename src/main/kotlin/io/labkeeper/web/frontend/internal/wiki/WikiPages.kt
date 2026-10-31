package io.labkeeper.web.frontend.internal.wiki

import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component

/** HTML pages of the on-site documentation, served under `/wiki/`. */
@Component
class WikiPages {
    val paths: List<String> = PathMatchingResourcePatternResolver()
        .getResources("classpath*:wiki/**/*.html")
        .mapNotNull { resource ->
            val uri = resource.uri.toString().replace('\\', '/')
            val marker = "/wiki/"
            val index = uri.lastIndexOf(marker)
            if (index < 0) null else uri.substring(index + marker.length)
        }
        .filter { it.endsWith(".html") && !it.startsWith("_") && !it.contains("/_") }
        .distinct()
        .sortedWith(compareBy({ if (it == "index.html") 0 else 1 }, { it }))
}
