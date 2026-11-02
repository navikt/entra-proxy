const {createServer} = require('node:http');
const {readFile} = require('node:fs/promises');
const {extname, resolve, sep} = require('node:path');

const roots = [
    resolve('build/generated/frontend/static'),
    resolve('src/main/resources/static')
];
const contentTypes = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.svg': 'image/svg+xml',
    '.png': 'image/png',
    '.gif': 'image/gif'
};

async function serve(request, response) {
    const pathname = new URL(request.url, 'http://127.0.0.1:4173').pathname;
    const asset = pathname === '/' ? 'index.html' : pathname.slice(1);
    for (const root of roots) {
        const file = resolve(root, asset);
        if (!file.startsWith(root + sep)) continue;
        try {
            const body = await readFile(file);
            response.writeHead(200, {
                'Content-Type': contentTypes[extname(file)] ?? 'application/octet-stream',
                'Cache-Control': 'no-store'
            });
            response.end(body);
            return;
        } catch (error) {
            if (error.code !== 'ENOENT' && error.code !== 'EISDIR') throw error;
        }
    }
    response.writeHead(404);
    response.end('Not found');
}

createServer((request, response) => {
    serve(request, response).catch(error => {
        console.error(error);
        response.writeHead(500);
        response.end('Static test server failed');
    });
}).listen(4173, '127.0.0.1');
