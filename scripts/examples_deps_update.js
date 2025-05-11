// Update the Scittlet dependencies in the example HTML files with the
// corresponding <scittlet key> dependencies from the catalog.
//
// This script replaces the dependencies in the HTML files enclosed by:
//
//  <!-- Scittlet dependencies: <scittlet key> -->
//  ... (existing dependencies)
//  <!-- Scittlet dependencies: end -->

const fs = require('fs');
const path = require('path');
let catalog = null;

const inputDir = 'examples';

try {
  const data = fs.readFileSync('catalog.json', 'utf8');
  catalog = JSON.parse(data);
  console.log(catalog);
} catch (err) {
  console.error('Error reading or parsing the catalog file:', err);
}

function processDirectory(dirPath) {
  const entries = fs.readdirSync(dirPath, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);

    if (entry.isDirectory()) {
      processDirectory(fullPath);
    } else if (entry.isFile() && fullPath.endsWith('.html')) {
      processFile(fullPath);
    }
  }
}

function processFile(filePath) {
  const original = fs.readFileSync(filePath, 'utf-8');

  const pattern = /^([ \t]*)<!-- Scittlet dependencies: ([\w.\-]+) -->[\s\S]*?<!-- Scittlet dependencies: end -->/gm;

  let changed = false;

  const updated = original.replace(pattern, (match, indent, key) => {
    const deps = catalog[key]["deps"];
    if (!deps) {
      console.warn(`No catalog entry for key "${key}" in ${filePath}`);
      return match;
    }

    const newBlock = [
      `${indent}<!-- Scittlet dependencies: ${key} -->`,
      ...deps.map(line => indent + line),
      `${indent}<!-- Scittlet dependencies: end -->`
    ].join('\n');

    changed = true;
    return newBlock;
  });

  if (changed && updated !== original) {
    fs.writeFileSync(filePath, updated, 'utf-8');
    console.log(`Updated: ${filePath}`);
  }
}

processDirectory(inputDir);
