export const EXAMPLE_ORIGIN = 'https://labkeeper.io';
export const EXAMPLE_TYPES = ['compute', 'latex', 'markdown'];
const SEGMENT_TYPES = [
    'compute',
    'computational',
    'latex',
    'markdown',
    'md',
    'asciimath',
    'mermaid',
    'plantuml',
    'drawio',
];
const PARAMETER_NAMES = [
    'visible',
    'hideAssignment',
    'hideAssignmentWithValues',
    'hideValue',
    'hideGeneralFormula',
    'hideInflAssignment',
    'hideInflAssignmentWithValues',
    'hideInfl',
];

function assertSegment(segment, slug, index) {
    const place = `${slug}: example segment ${index + 1}`;
    if (!segment || typeof segment !== 'object' || Array.isArray(segment)) {
        throw new Error(`${place} must be an object`);
    }
    const unknown = Object.keys(segment).filter((key) => !['type', 'text', 'parameters'].includes(key));
    if (unknown.length || !SEGMENT_TYPES.includes(segment.type)) {
        throw new Error(`${place} has an unknown type or field`);
    }
    if (typeof segment.text !== 'string' || !segment.text.trim()) {
        throw new Error(`${place} must contain non-empty text`);
    }
    if (segment.parameters == null) return;
    if (typeof segment.parameters !== 'object' || Array.isArray(segment.parameters)) {
        throw new Error(`${place} parameters must be an object`);
    }
    for (const [name, value] of Object.entries(segment.parameters)) {
        if (!PARAMETER_NAMES.includes(name) || typeof value !== 'boolean') {
            throw new Error(`${place} parameter ${name} must be true or false`);
        }
    }
}

/** Article links carry the example slug. The program stays in the article YAML. */
export function buildExampleUrl(example, slug = 'article') {
    if (!slug.trim()) throw new Error(`${slug}: example slug is empty`);
    if (Array.isArray(example)) {
        if (!example.length) throw new Error(`${slug}: add an editor example`);
        example.forEach((segment, index) => assertSegment(segment, slug, index));
    } else if (!example || typeof example !== 'object') {
        throw new Error(`${slug}: add an editor example`);
    } else {
        const keys = Object.keys(example);
        if (!keys.length || keys.some((key) => !EXAMPLE_TYPES.includes(key))) {
            throw new Error(`${slug}: example keys must be compute, latex or markdown`);
        }
        for (const type of EXAMPLE_TYPES) {
            if (!Object.hasOwn(example, type)) continue;
            if (typeof example[type] !== 'string' || !example[type].trim()) {
                throw new Error(`${slug}: ${type} must contain non-empty text`);
            }
        }
    }
    return `/project/default?example=${encodeURIComponent(slug)}`;
}
