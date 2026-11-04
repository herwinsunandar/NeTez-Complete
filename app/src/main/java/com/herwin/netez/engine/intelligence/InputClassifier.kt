package com.herwin.netez.engine.intelligence

import android.net.Uri
import android.util.Patterns
import java.util.Locale

object InputClassifier {

    private val emailPattern = Patterns.EMAIL_ADDRESS
    private val domainPattern = Regex("""^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,63}$""")

    fun classifyAndNormalize(rawInput: String): ParsedTarget {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return ParsedTarget(
                rawInput = rawInput,
                type = TargetType.INVALID,
                isValid = false,
                errorMessage = "Input cannot be empty. Please enter a domain, URL, or email."
            )
        }

        // 1. Check Email Address
        if (emailPattern.matcher(trimmed).matches()) {
            val parts = trimmed.split("@")
            val user = parts[0]
            val emailDomain = parts.getOrNull(1)?.lowercase(Locale.ROOT) ?: ""
            return ParsedTarget(
                rawInput = trimmed,
                type = TargetType.EMAIL,
                emailUser = user,
                emailDomain = emailDomain,
                host = emailDomain,
                baseDomain = extractBaseDomain(emailDomain),
                isValid = true
            )
        }

        // 2. Check HTTP / HTTPS URL
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return parseUrl(trimmed)
        }

        // 3. Check Domain / Hostname or IP
        val cleanedDomain = trimmed.lowercase(Locale.ROOT)
            .removePrefix("www.")
            .split("/")[0]
            .split(":")[0]

        if (domainPattern.matches(cleanedDomain) || Patterns.IP_ADDRESS.matcher(cleanedDomain).matches()) {
            return ParsedTarget(
                rawInput = trimmed,
                type = TargetType.DOMAIN,
                host = cleanedDomain,
                baseDomain = extractBaseDomain(cleanedDomain),
                scheme = "https",
                isValid = true
            )
        }

        // Fallback: Attempt URL parsing if contains slash
        if (trimmed.contains("/")) {
            return parseUrl("https://$trimmed")
        }

        return ParsedTarget(
            rawInput = trimmed,
            type = TargetType.INVALID,
            isValid = false,
            errorMessage = "Invalid input format. Example valid targets: 'example.com', 'https://example.com/login', or 'user@example.com'."
        )
    }

    private fun parseUrl(urlStr: String): ParsedTarget {
        return try {
            val uri = Uri.parse(urlStr)
            val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: "https"
            val rawHost = uri.host?.lowercase(Locale.ROOT) ?: ""

            if (rawHost.isBlank()) {
                ParsedTarget(
                    rawInput = urlStr,
                    type = TargetType.INVALID,
                    isValid = false,
                    errorMessage = "Invalid URL format: Unable to extract domain or host name."
                )
            } else {
                val host = rawHost.removePrefix("www.")
                val path = uri.path ?: "/"
                ParsedTarget(
                    rawInput = urlStr,
                    type = TargetType.URL,
                    scheme = scheme,
                    host = host,
                    baseDomain = extractBaseDomain(host),
                    urlPath = path,
                    isValid = true
                )
            }
        } catch (e: Exception) {
            ParsedTarget(
                rawInput = urlStr,
                type = TargetType.INVALID,
                isValid = false,
                errorMessage = "Invalid URL syntax: ${e.localizedMessage ?: "Parsing failed"}"
            )
        }
    }

    fun extractBaseDomain(host: String): String {
        if (host.isBlank() || Patterns.IP_ADDRESS.matcher(host).matches()) return host
        val parts = host.split(".")
        if (parts.size <= 2) return host

        // Handle common country second-level domains e.g. .co.id, .com.au, .org.uk
        val secondLast = parts[parts.size - 2]
        if (secondLast.length <= 3 && (secondLast == "co" || secondLast == "com" || secondLast == "org" || secondLast == "net" || secondLast == "go" || secondLast == "ac")) {
            return if (parts.size >= 3) {
                "${parts[parts.size - 3]}.${parts[parts.size - 2]}.${parts[parts.size - 1]}"
            } else host
        }

        return "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
    }
}
