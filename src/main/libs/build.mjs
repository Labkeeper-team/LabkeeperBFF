import { cpSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import * as esbuild from 'esbuild';

const root = dirname(fileURLToPath(import.meta.url));
const outDir = resolve(root, '../../../build/generated/libs');
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

await esbuild.build({
    absWorkingDir: root,
    entryPoints: ['src/katex.js'],
    bundle: true,
    format: 'iife',
    outfile: resolve(outDir, 'js/katex.js'),
    minify: true,
    legalComments: 'none',
});

const require = createRequire(import.meta.url);
const katexDist = resolve(dirname(require.resolve('katex/package.json')), 'dist');
cpSync(resolve(katexDist, 'katex.min.css'), resolve(outDir, 'css/katex.css'));
cpSync(resolve(katexDist, 'fonts'), resolve(outDir, 'css/fonts'), { recursive: true });
