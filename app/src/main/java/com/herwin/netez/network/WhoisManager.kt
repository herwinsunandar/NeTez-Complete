package com.herwin.netez.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

data class IpInfo(
    val query: String,
    val status: String,
    val country: String,
    val countryCode: String,
    val regionName: String,
    val city: String,
    val zip: String,
    val lat: Double,
    val lon: Double,
    val timezone: String,
    val isp: String,
    val org: String,
    val asn: String,
    val reverse: String,
    val mobile: Boolean,
    val proxy: Boolean,
    val hosting: Boolean
)

data class DomainInfo(
    val domain: String,
    val handle: String,
    val status: List<String>,
    val nameservers: List<String>,
    val creationDate: String,
    val expirationDate: String,
    val registrar: String,
    val raw: String
)

sealed class WhoisResult {
    data class Ip(val info: IpInfo) : WhoisResult()
    data class Domain(val info: DomainInfo) : WhoisResult()
    data class Error(val message: String) : WhoisResult()
}

object WhoisManager {
    fun isIpAddress(query: String): Boolean {
        val ipv4Pattern = Regex("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")
        val ipv6Pattern = Regex("([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])")
        return query.matches(ipv4Pattern) || query.matches(ipv6Pattern) || query.contains(":")
    }

    suspend fun getInfo(query: String): WhoisResult = withContext(Dispatchers.IO) {
        try {
            val trimmedQuery = query.trim()
            val isIp = isIpAddress(trimmedQuery)
            
            val urlString = if (isIp) {
                "http://ip-api.com/json/$trimmedQuery?fields=66846719"
            } else {
                "https://rdap.org/domain/${trimmedQuery.lowercase()}"
            }

            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.instanceFollowRedirects = true
            
            if (conn.responseCode !in 200..299) {
                return@withContext WhoisResult.Error("Server returned code ${conn.responseCode}. Make sure the domain/IP is valid.")
            }

            val scanner = Scanner(conn.inputStream)
            val sb = StringBuilder()
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine())
            }
            
            val rawJson = sb.toString()
            if (isIp) {
                parseIpInfo(rawJson)
            } else {
                parseDomainInfo(rawJson)
            }
        } catch (e: Exception) {
            WhoisResult.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }

    private fun parseIpInfo(json: String): WhoisResult {
        val obj = JSONObject(json)
        if (obj.getString("status") == "fail") {
            return WhoisResult.Error(obj.optString("message", "IP lookup failed"))
        }
        
        return WhoisResult.Ip(IpInfo(
            query = obj.optString("query", ""),
            status = obj.optString("status", ""),
            country = obj.optString("country", ""),
            countryCode = obj.optString("countryCode", ""),
            regionName = obj.optString("regionName", ""),
            city = obj.optString("city", ""),
            zip = obj.optString("zip", ""),
            lat = obj.optDouble("lat", 0.0),
            lon = obj.optDouble("lon", 0.0),
            timezone = obj.optString("timezone", ""),
            isp = obj.optString("isp", ""),
            org = obj.optString("org", ""),
            asn = obj.optString("as", ""),
            reverse = obj.optString("reverse", ""),
            mobile = obj.optBoolean("mobile", false),
            proxy = obj.optBoolean("proxy", false),
            hosting = obj.optBoolean("hosting", false)
        ))
    }

    private fun parseDomainInfo(json: String): WhoisResult {
        val obj = JSONObject(json)
        
        val ldhName = obj.optString("ldhName", "Unknown")
        val handle = obj.optString("handle", "-")
        
        val statusList = mutableListOf<String>()
        val statusArray = obj.optJSONArray("status")
        if (statusArray != null) {
            for (i in 0 until statusArray.length()) {
                statusList.add(statusArray.getString(i))
            }
        }

        val nameservers = mutableListOf<String>()
        val nsArray = obj.optJSONArray("nameservers")
        if (nsArray != null) {
            for (i in 0 until nsArray.length()) {
                nameservers.add(nsArray.getJSONObject(i).optString("ldhName", ""))
            }
        }

        var registrar = "-"
        val entities = obj.optJSONArray("entities")
        if (entities != null) {
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val roles = entity.optJSONArray("roles")
                if (roles != null) {
                    for (j in 0 until roles.length()) {
                        if (roles.getString(j) == "registrar") {
                            val vcard = entity.optJSONArray("vcardArray")
                            if (vcard != null && vcard.length() > 1) {
                                val fields = vcard.getJSONArray(1)
                                for (k in 0 until fields.length()) {
                                    val field = fields.getJSONArray(k)
                                    if (field.getString(0) == "fn") {
                                        registrar = field.getString(3)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        var created = "-"
        var expired = "-"
        val events = obj.optJSONArray("events")
        if (events != null) {
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                val action = event.optString("eventAction", "")
                val date = event.optString("eventDate", "")
                if (action == "registration") created = date
                if (action == "expiration") expired = date
            }
        }

        return WhoisResult.Domain(DomainInfo(
            domain = ldhName,
            handle = handle,
            status = statusList,
            nameservers = nameservers,
            creationDate = created,
            expirationDate = expired,
            registrar = registrar,
            raw = json
        ))
    }
}
