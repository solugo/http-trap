@file:JvmName("HttpTrap")

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


fun main() {
    val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX")
    val sessions = ConcurrentHashMap<String, WebSocketSession>()
    embeddedServer(CIO, port = System.getenv("PORT")?.toInt() ?: 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(1)
            timeout = Duration.ofSeconds(1)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        install(Routing) {
            staticResources("/", "static")

            webSocket("/ws/{id}") {
                val id = call.parameters["id"] ?: throw NotFoundException()
                sessions[id] = this
                incoming.consumeEach {
                }
                sessions.remove(id)
            }

            route("/") {
                handle {
                    call.respondRedirect("./watch/${UUID.randomUUID()}")
                }
            }

            route("/watch/{id}") {
                handle {
                    call.respondHtml {
                        head {
                            link(rel = "stylesheet", href = "/styles.css")
                            script(src = "/process.js") {}
                        }
                        body {
                            div { id = "url" }
                            div { id = "requests" }

                            script {
                                unsafe {
                                    +"process()"
                                }
                            }
                        }
                    }
                }
            }
            route("/callback/{watchId}") {
                handle {
                    val id = call.parameters.getAll("watchId")?.lastOrNull() ?: throw NotFoundException()

                    val session = sessions[id] ?: throw NotFoundException()

                    val body = call.receiveText()

                    val html = buildString {
                        appendHTML().apply {
                            div {
                                classes = setOf("location")

                                call.request.httpMethod.value.also { method ->
                                    span {
                                        classes = setOf("method")
                                        +method
                                    }
                                }

                                call.request.uri.also { uri ->
                                    span {
                                        classes = setOf("uri")
                                        +uri
                                    }
                                }

                                OffsetDateTime.now().format(dateTimeFormat).also { timestamp ->
                                    span {
                                        classes = setOf("timestamp")
                                        +timestamp
                                    }
                                }

                            }

                            table {
                                call.request.queryParameters.toMap().takeUnless { it.isEmpty() }?.also {
                                    div {
                                        classes = setOf("query", "table")
                                        div {
                                            classes = setOf("table-header")
                                            +"Query Parameters"
                                        }
                                        it.forEach { (parameterName, parameterValues) ->
                                            parameterValues.forEach { parameterValue ->
                                                div { +parameterName }
                                                div { +parameterValue }
                                            }
                                        }
                                    }
                                }
                                call.request.headers.toMap().takeUnless { it.isEmpty() }?.also {
                                    div {
                                        classes = setOf("query", "table")
                                        div {
                                            classes = setOf("table-header")
                                            +"Headers"
                                        }
                                        it.forEach { (headerName, headerValues) ->
                                            headerValues.forEach { headerValue ->
                                                div { +headerName }
                                                div { +headerValue }
                                            }
                                        }
                                    }
                                }

                                body.takeUnless { it.isEmpty() }?.also { body ->

                                    tbody {
                                        classes = setOf("body")
                                        tr {
                                            th {
                                                colSpan = "2"
                                                +"Headers"
                                            }
                                        }
                                        tr {
                                            td {
                                                colSpan = "2"
                                                +body
                                            }
                                        }
                                    }

                                    div {
                                        classes = setOf("body")
                                        pre {
                                            +body
                                        }
                                    }
                                }
                            }


                        }
                    }

                    session.send(html)

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }.start(
        wait = true,
    )
}
