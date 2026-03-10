#!/usr/bin/env node

function stripTags(input) {
  return input.replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim();
}

function extractTitle(html) {
  const match = html.match(/<title[^>]*>([\s\S]*?)<\/title>/i);
  return match ? stripTags(match[1]) : null;
}

function extractLinks(html, limit = 10) {
  const links = [];
  const linkRegex = /<a\s+[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)<\/a>/gi;

  let m;
  while ((m = linkRegex.exec(html)) !== null && links.length < limit) {
    links.push({
      href: m[1],
      text: stripTags(m[2])
    });
  }

  return links;
}

async function run() {
  const url = process.argv[2];
  if (!url) {
    console.error('Missing URL argument. Usage: node crawler/crawl.js <url>');
    process.exit(1);
  }

  let response;
  try {
    response = await fetch(url, { redirect: 'follow' });
  } catch (error) {
    console.error(`Failed to fetch URL: ${error.message}`);
    process.exit(1);
  }

  if (!response.ok) {
    console.error(`Fetch failed with status ${response.status}`);
    process.exit(1);
  }

  const html = await response.text();

  const payload = {
    url,
    title: extractTitle(html),
    links: extractLinks(html, 10),
    fetchedAt: new Date().toISOString()
  };

  process.stdout.write(JSON.stringify(payload, null, 2));
}

run();
