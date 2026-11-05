import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';

export default tseslint.config(
    {ignores: ['build/**', 'node_modules/**', '**/*.cjs']},
    eslint.configs.recommended,
    ...tseslint.configs.recommended,
    {
        files: ['src/main/typescript/**/*.ts'],
        extends: [tseslint.configs.recommendedTypeChecked],
        languageOptions: {
            parserOptions: {project: './tsconfig.json'}
        },
        rules: {
            '@typescript-eslint/no-floating-promises': 'error',
            '@typescript-eslint/no-non-null-assertion': 'error',
            'eqeqeq': 'error',
            'prefer-const': 'error'
        }
    }
);
