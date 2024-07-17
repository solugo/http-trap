function process() {
    const requestUri = window.location.href;
    const wsUri = requestUri.replace("/watch", "/ws");
    const callbackUri = requestUri.replace("/watch", "/callback")

    const requestsElement = document.getElementById("requests")
    const urlElement = document.getElementById("url")
    urlElement.innerText = callbackUri
    urlElement.style.backgroundColor = '#960';
    const webSocket = new WebSocket(wsUri);
    webSocket.onopen = function () {
        urlElement.style.backgroundColor = '#090';
    }
    webSocket.onclose = function () {
        urlElement.style.backgroundColor = '#900';
        setTimeout(function () {
            process(url)
        }, 1000)
    }
    webSocket.onmessage = function (event) {
        const request = document.createElement("div");
        request.className = "request";
        request.innerHTML = event.data;
        requestsElement.prepend(request);
    }
}
