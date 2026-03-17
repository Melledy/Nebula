@echo off

REM -- We utilize mitmproxy's local redirector mode to *only* proxy Stella Sora.
REM -- https://www.mitmproxy.org/posts/local-capture/windows/
mitmweb -k -m wireguard --set stream_large_bodies=3m --set block_global=false --mode local:StellaSora.exe -s proxy.py

pause >nul