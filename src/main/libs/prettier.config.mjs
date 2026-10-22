/** Same style as the editor. Resource pages, scripts, and article YAML are formatted from this folder. */
export default {
    semi: true,
    singleQuote: true,
    tabWidth: 4,
    trailingComma: 'es5',
    bracketSpacing: true,
    bracketSameLine: false,
    endOfLine: 'auto',
    overrides: [
        {
            files: '*.{yaml,yml}',
            options: {
                tabWidth: 2,
                proseWrap: 'preserve',
            },
        },
    ],
};
