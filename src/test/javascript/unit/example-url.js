export const MAX_EXAMPLE_URL_LENGTH = 2000;
export const EXAMPLE_ORIGIN = 'https://labkeeper.io';
export const EXAMPLE_TYPES = ['compute', 'latex', 'markdown'];

function javaUrlEncode(value) {
    let out = '';
    for (const byte of Buffer.from(value, 'utf8')) {
        const ch = String.fromCharCode(byte);
        if (/[a-zA-Z0-9]/.test(ch) || ch === '-' || ch === '_' || ch === '.' || ch === '*') out += ch;
        else if (ch === ' ') out += '+';
        else out += `%${byte.toString(16).toUpperCase().padStart(2, '0')}`;
    }
    return out;
}

/** Same query string as `BlogCatalog.exampleUrl`: Java `URLEncoder`, then a browser-decodable link. */
export function buildExampleUrl(example, slug = 'article') {
    if (!example || typeof example !== 'object' || Array.isArray(example)) {
        throw new Error(`${slug}: add an editor example`);
    }
    const keys = Object.keys(example);
    if (!EXAMPLE_TYPES.some((type) => Object.hasOwn(example, type)) ||
        keys.some((key) => !EXAMPLE_TYPES.includes(key) && key !== 'computeVisible')) {
        throw new Error(`${slug}: example keys must be compute, latex, markdown or computeVisible`);
    }
    const params = [['open', 'latex' in example || 'compute' in example ? 'latex' : 'markdown']];
    for (const type of EXAMPLE_TYPES) {
        if (!Object.hasOwn(example, type)) continue;
        if (typeof example[type] !== 'string' || !example[type].trim()) {
            throw new Error(`${slug}: ${type} must contain non-empty text`);
        }
        params.push([type, example[type]]);
    }
    if (Object.hasOwn(example, 'computeVisible')) {
        if (typeof example.computeVisible !== 'boolean' || !Object.hasOwn(example, 'compute')) {
            throw new Error(`${slug}: computeVisible must be a boolean and requires compute`);
        }
        params.push(['computeVisible', String(example.computeVisible)]);
    }
    const url = `/project/default?${params.map(([name, value]) => `${name}=${javaUrlEncode(value)}`).join('&')}`;
    const length = EXAMPLE_ORIGIN.length + url.length;
    if (length > MAX_EXAMPLE_URL_LENGTH) {
        throw new Error(`${slug}: encoded example URL is ${length} characters (limit ${MAX_EXAMPLE_URL_LENGTH})`);
    }
    return url;
}
