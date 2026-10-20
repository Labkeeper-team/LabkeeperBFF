import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import * as esbuild from 'esbuild';

const root = dirname(fileURLToPath(import.meta.url));
const outDir = resolve(root, '../build/landing');
mkdirSync(outDir, { recursive: true });

await esbuild.build({
    absWorkingDir: root,
    entryPoints: ['src/swiper.js'],
    bundle: true,
    format: 'iife',
    outfile: resolve(outDir, 'js/swiper.js'),
    minify: true,
    legalComments: 'none',
});

await esbuild.build({
    absWorkingDir: root,
    entryPoints: ['src/swiper.css'],
    bundle: true,
    outfile: resolve(outDir, 'css/swiper.css'),
    minify: true,
    legalComments: 'none',
});
