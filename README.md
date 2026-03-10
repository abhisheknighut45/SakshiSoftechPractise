# Java + JavaScript Web Crawler Server

This project gives you a simple **Java server** that exposes an HTTP endpoint and uses a **JavaScript crawler** to fetch and parse website data.

## What it does

- Java server runs on `http://localhost:8080`
- Endpoint: `GET /crawl?url=<target-url>`
- Java starts a Node.js script (`crawler/crawl.js`) to crawl the page
- Response is returned as JSON (title + first 10 links)

## Requirements

- Java 11+
- Node.js 18+ (for built-in `fetch`)

## Run

```bash
javac server/CrawlerServer.java
java -cp server CrawlerServer
```

In another terminal:

```bash
curl "http://localhost:8080/crawl?url=https%3A%2F%2Fexample.com"
```

Health check:

```bash
curl "http://localhost:8080/health"
```

## Files

- `server/CrawlerServer.java` - Java HTTP server and Node process launcher
- `crawler/crawl.js` - JavaScript crawler/parser
