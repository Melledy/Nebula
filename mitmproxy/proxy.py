from mitmproxy import ctx, http
from mitmproxy.proxy import layer

# If you use any client other than the global client,
# add their appropriate endpoints here (SDK and server)
TARGET_HOSTS = [
    "en-sdk-api.yostarplat.com",
    "nova.stellasora.global",
]

# Nebula server address
SERVER_HOST = "127.0.0.1"
SERVER_PORT = 80        # Make sure to modify this according to your config.json file!

def request(flow: http.HTTPFlow) -> None:
    if flow.request.pretty_host in TARGET_HOSTS:
        flow.request.scheme = 'http'
        flow.request.host = SERVER_HOST
        flow.request.port = SERVER_PORT
        return
