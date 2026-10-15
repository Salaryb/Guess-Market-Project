/*
 * Guess Market web client - a tiny web server (Node.js built-in modules only, nothing to install).
 *  - Serves the web client (the "public" folder) on http://localhost:3000
 *  - Forwards every request under /guessmarket/... to the existing Tomcat server (http://localhost:8080),
 *    so the browser talks to a single address and the session cookie works normally.
 *    The Tomcat server itself is not changed in any way.
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = Number(process.env.PORT) || 3000;
const TOMCAT_HOST = 'localhost';
const TOMCAT_PORT = 8080;
const API_PREFIX = '/guessmarket/';
const PUBLIC_DIR = path.join(__dirname, 'public');
const CONTENT_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
};

function forwardToTomcat(req, res) {
  const upstream = http.request({
    host: TOMCAT_HOST,
    port: TOMCAT_PORT,
    path: req.url,
    method: req.method,
    headers: { ...req.headers, host: `${TOMCAT_HOST}:${TOMCAT_PORT}` },
  }, (tomcatResponse) => {
    res.writeHead(tomcatResponse.statusCode, tomcatResponse.headers);
    tomcatResponse.pipe(res);
  });
  upstream.on('error', () => {
    res.writeHead(502, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ message: 'Cannot reach the Guess Market server at http://localhost:8080/guessmarket. Is Tomcat running?' }));
  });
  req.pipe(upstream);
}

function serveStatic(req, res) {
  const urlPath = decodeURIComponent(req.url.split('?')[0]);
  const relative = urlPath === '/' ? 'index.html' : urlPath.replace(/^\/+/, '');
  const filePath = path.normalize(path.join(PUBLIC_DIR, relative));
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }
  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('Not found');
      return;
    }
    res.writeHead(200, {
      'Content-Type': CONTENT_TYPES[path.extname(filePath)] || 'application/octet-stream',
      'Cache-Control': 'no-cache',
    });
    res.end(content);
  });
}

http.createServer((req, res) => {
  if (req.url.startsWith(API_PREFIX)) {
    forwardToTomcat(req, res);
  } else {
    serveStatic(req, res);
  }
}).listen(PORT, () => {
  console.log('Guess Market web client is running.');
  console.log(`Open http://localhost:${PORT} in your browser.`);
  console.log('(Tomcat with guessmarket.war must be running on http://localhost:8080)');
  console.log('Keep this window open. Close it (or press Ctrl+C) to stop the web client.');
});
